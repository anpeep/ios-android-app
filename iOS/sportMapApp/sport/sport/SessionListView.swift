import SwiftUI
import Foundation
import CoreLocation
import Combine

struct SessionListView: View {
    @State var sessions: [GpsSessionListItem] = []
    @State private var isLoading = true
        @State private var showRenameAlert = false
    @State private var sessionToRename: GpsSessionListItem?
    @State private var newName = ""
    @State private var newDescription = ""

    var body: some View {
        NavigationStack {
            Group {
                if isLoading {
                    ProgressView("Loading sessions...")
                } else if sessions.isEmpty {
                    VStack(spacing: 20) {
                        Image(systemName: "figure.run.circle")
                            .font(.system(size: 60))
                            .foregroundColor(.gray)
                        Text("No sessions yet").font(.headline)
                        Text("Start a new tracking session to see it here.")
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                    }
                } else {
                    List(sessions) { session in
                        NavigationLink(destination: SessionDetailView(sessionId: session.id)) {
                            VStack(alignment: .leading) {
                                Text(session.name ?? "Untitled Session")
                                    .font(.headline)
                                
                                Text(session.description ?? "No description")
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                                
                                Text("Distance: \(session.distance, specifier: "%.1f") m")
                                    .font(.caption2)
                                    .foregroundColor(.blue)
                            }
                        }
                        .swipeActions(edge: .trailing) {
                            Button(role: .destructive) {
                                delete(session)
                            } label: {
                                Label("Delete", systemImage: "trash")
                            }
                        }
                        .swipeActions(edge: .leading) {
                            Button {
                                sessionToRename = session
                                newName = session.name ?? ""
                                newDescription = session.description ?? ""
                                showRenameAlert = true
                            } label: {
                                Label("Rename", systemImage: "pencil")
                            }
                            .tint(.orange)
                        }
                    }
                }
            }
            .navigationTitle("My Sessions")
            .task {
                await fetchSessions()
            }
            .refreshable {
                await fetchSessions()
            }
            .alert("Rename Session", isPresented: $showRenameAlert) {
                TextField("Name", text: $newName)
                TextField("Description", text: $newDescription)
                Button("Save") {
                    if let session = sessionToRename {
                        // 1. Dismiss the alert logic first
                        showRenameAlert = false
                        
                        // 2. Wait for the animation to finish (0.5s)
                        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
                            rename(session)
                        }
                    }
            
                    
                }
                
            }}}
    
            func delete(_ session: GpsSessionListItem) {
                    Task {
                        do {
                            try await GpsApiService().deleteSession(id: session.id)
                            // Update local UI
                            withAnimation {
                                sessions.removeAll { $0.id == session.id }
                            }
                            // Also remove from local storage so it doesn't fetch again
                            var ids = UserDefaults.standard.stringArray(forKey: "my_session_ids") ?? []
                            ids.removeAll { $0 == session.id }
                            UserDefaults.standard.set(ids, forKey: "my_session_ids")
                        } catch {
                            print("❌ Delete failed: \(error)")
                        }
                    }
                }
    func rename(_ session: GpsSessionListItem) {
        Task {
            do {
                try await GpsApiService().updateSession(
                    sessionId: session.id,
                    name: newName,
                    description: newDescription,
                    recordedAt: session.recordedAt ?? "",
                    sessionTypeId: "00000000-0000-0000-0000-000000000001"                )
                await fetchSessions()
            } catch {
                print("❌ Rename failed: \(error)")
            }
        }
    }
    
    func fetchSessions() async {
        isLoading = true
        do {
            let ids = UserDefaults.standard.stringArray(forKey: "my_session_ids") ?? []
            
            var loaded: [GpsSessionListItem] = []
            
            for id in ids {
                do {
                    let session = try await GpsApiService().getSession(id: id)
                    loaded.append(session)
                } catch {
                    print("❌ Failed to load session \(id)")
                }
            }
            let formatter = ISO8601DateFormatter()
            formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
            
            self.sessions = loaded.sorted {
                let d0 = formatter.date(from: $0.recordedAt ?? "") ?? .distantPast
                let d1 = formatter.date(from: $1.recordedAt ?? "") ?? .distantPast
                return d0 > d1
            }
            isLoading = false
        }
    }
}
