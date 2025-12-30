import SwiftUI
import Combine


// MARK: - Models

enum Player: Int {
    case none = 0
    case red
    case blue

    var color: Color {
        switch self {
        case .none: return .gray
        case .red: return .red
        case .blue: return .blue
        }
    }

    var next: Player { self == .red ? .blue : .red }
    var name: String { self == .red ? "Red" : "Blue" }
}

enum Difficulty: String, CaseIterable {
    case easy = "Easy"
    case medium = "Medium"
    case hard = "Hard"

    var size: Int {
        switch self {
        case .easy: return 4
        case .medium: return 6
        case .hard: return 10
        }
    }

    var winCount: Int {
        switch self {
        case .easy: return 3
        case .medium: return 4
        case .hard: return 5
        }
    }
}

// MARK: - Game ViewModel

final class GameViewModel: ObservableObject {
    @Published var board: [[Player]]
    @Published var currentPlayer: Player = .red
    @Published var winningLine: [(Int, Int)] = []
    @Published var showEndPopup = false
    @Published var isDraw = false
    @Published var elapsedTime = 0
    @Published var shakeColumn: Int?

    var size: Int
    var winCount: Int
    private var timer: Timer?

    init(difficulty: Difficulty) {
        size = difficulty.size
        winCount = difficulty.winCount
        board = Array(repeating: Array(repeating: .none, count: size), count: size)
        startTimer()
    }

    // MARK: Timer
    func startTimer() {
        timer?.invalidate()
        elapsedTime = 0
        timer = Timer.scheduledTimer(withTimeInterval: 1, repeats: true) { _ in
            self.elapsedTime += 1
        }
    }

    func stopTimer() {
        timer?.invalidate()
    }

    // MARK: Game Logic
    func dropPiece(column: Int) {
        guard !showEndPopup else { return }

        for row in (0..<size).reversed() {
            if board[row][column] == .none {
                board[row][column] = currentPlayer

                if let win = checkWin(row: row, col: column) {
                    winningLine = win
                    stopTimer()
                    showEndPopup = true
                    return
                }

                if isBoardFull() {
                    isDraw = true
                    stopTimer()
                    showEndPopup = true
                    return
                }

                currentPlayer = currentPlayer.next
                return
            }
        }

        // Column full → shake
        withAnimation(.default) { shakeColumn = column }
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.4) {
            self.shakeColumn = nil
        }
    }
    func reset(with difficulty: Difficulty) {
        stopTimer()
        size = difficulty.size
        winCount = difficulty.winCount
        board = Array(repeating: Array(repeating: .none, count: size), count: size)
        currentPlayer = .red
        winningLine = []
        shakeColumn = nil
        showEndPopup = false
        isDraw = false
        startTimer()
    }

    func isBoardFull() -> Bool {
        board[0].allSatisfy { $0 != .none }
    }

    // MARK: Win Detection
    func checkWin(row: Int, col: Int) -> [(Int, Int)]? {
        let directions = [(1,0),(0,1),(1,1),(1,-1)]

        for (dx, dy) in directions {
            var line = [(row, col)]
            line += collect(row, col, dx, dy)
            line += collect(row, col, -dx, -dy)

            if line.count >= winCount {
                return Array(line.prefix(winCount))
            }
        }
        return nil
    }

    func collect(_ r: Int, _ c: Int, _ dx: Int, _ dy: Int) -> [(Int, Int)] {
        var r = r + dx
        var c = c + dy
        var result: [(Int, Int)] = []

        while r >= 0, r < size, c >= 0, c < size,
              board[r][c] == currentPlayer {
            result.append((r, c))
            r += dx
            c += dy
        }
        return result
    }
}

// MARK: - Root View

struct ContentView: View {
    @State private var selectedDifficulty: Difficulty?

    var body: some View {
        if let diff = selectedDifficulty {
            GameView(initialDifficulty: diff)

        } else {
            MenuView { selectedDifficulty = $0 }
        }
    }
}

// MARK: - Menu

struct MenuView: View {
    let onSelect: (Difficulty) -> Void
    
    var body: some View {
        VStack(spacing: 30) {
            Text("Connect Game")
                .font(.largeTitle)
                .bold()
            
            ForEach(Difficulty.allCases, id: \.self) { diff in
                Button(diff.rawValue) {
                    onSelect(diff)
                }
                .font(.title2)
                .buttonStyle(.borderedProminent)
            }
        }
    }
}


// MARK: - Game Screen
struct GameView: View {
    @State private var difficulty: Difficulty
    @StateObject private var vm: GameViewModel

    init(initialDifficulty: Difficulty) {
        _difficulty = State(initialValue: initialDifficulty)
        _vm = StateObject(wrappedValue: GameViewModel(difficulty: initialDifficulty))
    }

    var body: some View {
        GeometryReader { geo in
            let landscape = geo.size.width > geo.size.height

            if landscape {
                HStack {
                    statsView
                        .frame(width: geo.size.width * 0.3)
                    boardView
                }
            } else {
                VStack {
                    statsView
                        .frame(height: geo.size.height * 0.3)
                    boardView
                }
            }
        }
        .id(difficulty)
        .alert("Game Over", isPresented: $vm.showEndPopup) {
            ForEach(Difficulty.allCases, id: \.self) { diff in
                Button(diff.rawValue) {
                    difficulty = diff
                    vm.reset(with: diff)
                }
            }
        } message: {
            Text(
                vm.isDraw
                ? "Draw\nTime: \(vm.elapsedTime)s"
                : "\(vm.currentPlayer.name) won\nTime: \(vm.elapsedTime)s"
            )
        }
    }

    // MARK: - Stats View
    var statsView: some View {
        VStack(spacing: 12) {
            Text(vm.isDraw ? "Draw" : "Turn: \(vm.currentPlayer.name)")
                .font(.title2)
                .foregroundColor(vm.currentPlayer.color)

            Text("Time: \(vm.elapsedTime)s")
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    // MARK: - Board View
    var boardView: some View {
        GeometryReader { geo in
            let cellSize = min(
                geo.size.width / CGFloat(vm.size),
                geo.size.height / CGFloat(vm.size)
            )

            VStack(spacing: 4) {
                ForEach(0..<vm.size, id: \.self) { r in
                    HStack(spacing: 4) {
                        ForEach(0..<vm.size, id: \.self) { c in
                            Circle()
                                .fill(vm.board[r][c].color)
                                .overlay(
                                    vm.winningLine.contains(where: { $0 == (r,c) })
                                    ? Circle().stroke(.yellow, lineWidth: 3)
                                    : nil
                                )
                                .frame(
                                    width: cellSize - 6,
                                    height: cellSize - 6
                                )
                                .offset(x: vm.shakeColumn == c ? -6 : 0)
                                .onTapGesture {
                                    vm.dropPiece(column: c)
                                }
                        }
                    }
                }
            }
            .frame(
                width: cellSize * CGFloat(vm.size),
                height: cellSize * CGFloat(vm.size)
            )
            .centerInParent()
        }
    }
}

extension View {
    func centerInParent() -> some View {
        HStack {
            Spacer()
            VStack {
                Spacer()
                self
                Spacer()
            }
            Spacer()
        }
    }
}
