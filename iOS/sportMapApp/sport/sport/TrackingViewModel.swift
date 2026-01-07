//
//  TrackingViewModel.swift
//  sport
//
//  Created by Anne-Riin Peep on 06.01.2026.
//


import Foundation
import CoreLocation
import Combine

@MainActor
final class TrackingViewModel: ObservableObject {
    @Published var isTracking = false
    
    private let api = GpsApiService()
    private var timer: Timer?
    @Published var showEndSessionDialog = false
    @Published var showLogs = false
    @Published var sessionStartTime: String?
    @Published var sessionName = ""
    @Published var sessionDescription = ""

    // Internal state for the API
    private var sessionId: String?
    private var locTypeId: String?
    private var cpTypeId: String?
    private let gpsService = GpsApiService()
    // MARK: - Start Tracking
    func startTracking(locationVM: LocationManager) {
        Task {
            do {
                // 1. Fetch Types & Create Session
                let sessionTypes = try await api.getSessionTypes()
                let locationTypes = try await api.getLocationTypes()
                
                guard let sessionType = sessionTypes.first,
                      let loc = locationTypes.first(where: { $0.name == "LOC" }),
                      let cp = locationTypes.first(where: { $0.name == "CP" })
                else { return }
                
                self.locTypeId = loc.id
                self.cpTypeId = cp.id
                
                let session = try await api.createSession(type: sessionType)
                self.sessionId = session.id
                
                // 2. Start UI, Timer, and Live Activity on Main Thread
                await MainActor.run {
                    // IMPORTANT: Use the single 'now' for everything
                    locationVM.startTracking()
                    
                    // This starts your 10-second API upload timer
                    self.startTimer(locationVM: locationVM)
                    self.sessionStartTime = session.recordedAt

                    self.isTracking = true
                }

                print("✅ Tracking started with session: \(session.id)")
                
            } catch {
                print("❌ Start tracking failed: \(error)")
            }
        }
    }
    // MARK: - The Timer Logic
    private func startTimer(locationVM: LocationManager) {
        timer?.invalidate()
        timer = Timer.scheduledTimer(withTimeInterval: 10, repeats: true) { [weak self] _ in
            Task { @MainActor in
                // We are now on the Main Actor, it is safe to read locationVM properties
                guard let self = self, let latestLocation = locationVM.lastLocation else { return }
                await self.sendLocation(latestLocation)
            }
        }
    }
    func finishSession(locationVM: LocationManager) {
        Task {
            // 1. Always stop local tracking immediately
            locationVM.stopTracking()
            self.timer?.invalidate()
            self.timer = nil
            
            guard let sessionId = sessionId,
                    let startTime = sessionStartTime,
                let locTypeId = locTypeId else {
                print("⚠️ No Session ID found, closing dialog anyway")
                
                self.showEndSessionDialog = false
                self.isTracking = false
                return
            }

            do {
                // 2. Try the API call (Make sure this matches your Service function)
                try await gpsService.updateSession(
                    sessionId: sessionId,
                    name: sessionName,
                    description: sessionDescription,
                    recordedAt: startTime,
                    sessionTypeId: locTypeId,
                )
                print("✅ API Update Success")
            } catch {
                // 3. Catch the error so it doesn't "kill" the function
                print("❌ API Update failed: \(error)")
            }

            // 4. This MUST be outside the do-catch or after it to ensure the UI closes
            await MainActor.run {
                self.isTracking = false
                self.showEndSessionDialog = false
                self.sessionName = ""
                self.sessionDescription = ""
                self.sessionId = nil // Important: Clear for next session
                self.sessionStartTime = nil
            }
        }
    }

    private func sendLocation(_ location: CLLocation) async {
        guard let sessionId = sessionId, let locTypeId = locTypeId else { return }
        
        print("⬆️ Timer: Sending Location to backend")
        do {
            try await api.sendLocation(
                location: location,
                sessionId: sessionId,
                locationTypeId: locTypeId
            )
        } catch {
            print("❌ Timer: Failed to send location: \(error)")
        }
    }

    // MARK: - Checkpoints (Manual)
    func sendCheckpoint(locationVM: LocationManager) {
        guard let location = locationVM.lastLocation,
              let sessionId = sessionId,
              let cpTypeId = cpTypeId else { return }

        Task {
            try? await api.sendLocation(
                location: location,
                sessionId: sessionId,
                locationTypeId: cpTypeId
            )
            print("🚩 Checkpoint sent")
        }
    }

    func stopTracking(locationVM: LocationManager) {
        timer?.invalidate()
        timer = nil
        locationVM.stopGPS()
        isTracking = false
        sessionId = nil
    }
}
