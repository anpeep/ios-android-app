//
//  StatsPanel.swift
//  sport
//
//  Created by Anne-Riin Peep on 06.01.2026.
//
import Combine
import SwiftUI

struct StatsPanel: View {
    @ObservedObject var vm: LocationManager

    func formatTime(_ t: TimeInterval) -> String {
        let h = Int(t) / 3600
        let m = (Int(t) % 3600) / 60
        let s = Int(t) % 60
        return String(format: "%02d:%02d:%02d", h, m, s)
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("⏱ Duration: \(formatTime(vm.sessionDuration))")
            Text("📏 Distance: \(vm.distance, specifier: "%.0f") m")
            Text("⚡ Avg pace: \(vm.averagePace, specifier: "%.2f") s/m")

            Divider()

            Text("🏁 From CP: \(vm.distanceFromCheckpoint, specifier: "%.0f") m")
            Text("🏁 Pace CP: \(vm.paceSinceCheckpoint, specifier: "%.2f") s/m")

            Divider()

            Text("📍 From WP: \(vm.distanceFromWaypoint, specifier: "%.0f") m")
            Text("📍 Pace WP: \(vm.paceSinceWaypoint, specifier: "%.2f") s/m")
        }
        .font(.footnote)
        .padding(12)
        .background(.ultraThinMaterial)
        .cornerRadius(12)
    }
}
