//
//  SessionManager.swift
//  sport
//
//  Created by Anne-Riin Peep on 05.01.2026.
//


import Foundation
import CoreData
import Combine
import CoreLocation

final class SessionManager: ObservableObject {

    @Published var currentSessionId: UUID?
    @Published var isTracking = false

    func startSession() {
        currentSessionId = UUID()
        isTracking = true
    }

    func stopSession() {
        isTracking = false
        currentSessionId = nil
    }

    func addCP(_ location: CLLocation?) {
        guard let location else { return }
        // later: save to Core Data + backend
    }

    func addWP(_ location: CLLocation?) {
        guard let location else { return }
        // later: remove previous WP, add new
    }
}
