//
//  SessionListView.swift
//  sport
//
//  Created by Anne-Riin Peep on 06.01.2026.
//
import SwiftUI
import Foundation
import CoreLocation
import Combine
struct SessionListView: View {
    @State var sessions: [GpsSessionListItem] = []
    @State private var isLoading = true

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
                        Text("No sessions yet")
                            .font(.headline)
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
        }
    }

    func fetchSessions() async {
        isLoading = true
        defer { isLoading = false }
        guard let email = UserDefaults.standard.string(forKey: "user_email"),
              !email.isEmpty else {
            print("❌ NO EMAIL STORED")
            return
        }
        do {
            let email = UserDefaults.standard.string(forKey: "user_email") ?? ""
            var result = try await GpsApiService().getMySessions(email: email)

            // newest first
            result.sort { ($0.recordedAt ?? "") > ($1.recordedAt ?? "") }

            // only 10
            sessions = Array(result.prefix(10))

        } catch {
            print("❌ List Error: \(error)")
        }
    }
}
