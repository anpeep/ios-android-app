//
//  SessionDetailView.swift
//  sport
//
//  Created by Anne-Riin Peep on 06.01.2026.
//
import SwiftUI
import Foundation
import CoreLocation
import Combine

struct SessionDetailView: View {
    let sessionId: String
    @State private var session: GpsSessionListItem?
    @State private var isLoading = true

    var body: some View {
        VStack {
            if isLoading {
                ProgressView()
            } else if let session = session {
                List {
                    Section("Session Info") {
                        Text("Name: \(session.name ?? "N/A")")
                        Text("Description: \(session.description ?? "N/A")")
                        Text("Speed: \(Double(truncating: session.speed as NSNumber), specifier: "%.2f") km/h")
                    }
                }
            }
        }
        .navigationTitle("Session Details")
        .task {
            do {
                // Here we call the /{id} endpoint
                self.session = try await GpsApiService().getSessionDetails(id: sessionId)
            } catch {
                print("❌ Error fetching specific session: \(error)")
            }
            isLoading = false
        }
    }
}
