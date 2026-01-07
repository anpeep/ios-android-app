//
//  GpsApiService.swift
//  sport
//
//  Created by Anne-Riin Peep on 06.01.2026.
//


import Foundation
import CoreLocation
import UIKit

final class GpsApiService {
    
    // MARK: - Fetch Types
    
    func getSessionTypes() async throws -> [GpsSessionType] {
        try await APIClient.shared.request(path: "GpsSessionTypes")
    }
    
    func getLocationTypes() async throws -> [GpsLocationType] {
        try await APIClient.shared.request(path: "GpsLocationTypes")
    }
    func getSession(id: String) async throws -> GpsSessionListItem {
        return try await APIClient.shared.request(
            path: "GpsSessions/\(id)",
            method: "GET"
        )
    }
    func saveSessionId(_ id: String) {
        var ids = UserDefaults.standard.stringArray(forKey: "my_session_ids") ?? []
        if !ids.contains(id) {
            ids.append(id)
            UserDefaults.standard.set(ids, forKey: "my_session_ids")
        }
    }
    func getSessionLocations(sessionId: String) async throws -> [GpsLocationDTO] {
        try await APIClient.shared.request(
            path: "GpsLocations/Session/\(sessionId)",
            method: "GET"
        )
    }
    
    // MARK: - Create Session
    
    func createSession(type: GpsSessionType) async throws -> GpsSessionResponse {
        let dto = CreateGpsSessionRequest(
            name: "Training",
            description: "Auto recorded",
            gpsSessionTypeId: type.id,
            recordedAt: ISO8601DateFormatter().string(from: Date()),
            paceMin: type.paceMin,
            paceMax: type.paceMax
        )
        
        let session: GpsSessionResponse = try await APIClient.shared.request(
            path: "GpsSessions",
            method: "POST",
            body: dto
        )
        saveSessionId(session.id)
        return session
    }
    func deleteSession(id: String) async throws {
        let _: EmptyResponse = try await APIClient.shared.request(
            path: "GpsSessions/\(id)",
            method: "DELETE"
        )
    }
    func updateSession(
        sessionId: String,
        name: String,
        description: String,
        recordedAt: String,
        sessionTypeId: String
    ) async throws {
        
        // Create a concrete struct instead of [String: Any]
        let dto = UpdateSessionRequest(
            id: sessionId,
            name: name,
            description: description,
            recordedAt: recordedAt,
            paceMin: 0.0,
            paceMax: 0.0,
            gpsSessionTypeId: sessionTypeId
        )
        if let jsonData = try? JSONEncoder().encode(dto),
           let jsonString = String(data: jsonData, encoding: .utf8) {
            print("DEBUG JSON: \(jsonString)")
        }
        // Now 'dto' is Encodable, and the error disappears
        try await APIClient.shared.requestVoid(
            path: "GpsSessions/\(sessionId)",
            method: "PUT",
            body: dto
        )
        print("✅ Session updated successfully")
    }
    struct UpdateSessionRequest: Codable {
        let id: String
        let name: String
        let description: String
        let recordedAt: String
        let paceMin: Double
        let paceMax: Double
        let gpsSessionTypeId: String
    }
    
    func sendLocation(
        location: CLLocation,
        sessionId: String,
        locationTypeId: String
    ) async throws {
        
        // 1. Declare the identifier variable first
        var taskID: UIBackgroundTaskIdentifier = .invalid
        
        // 2. Assign it, using the variable inside the closure
        taskID = UIApplication.shared.beginBackgroundTask(withName: "SendLocation") {
            UIApplication.shared.endBackgroundTask(taskID)
            taskID = .invalid // Reset to invalid
        }
        
        let dto = CreateGpsLocationRequest(
            recordedAt: ISO8601DateFormatter().string(from: location.timestamp),
            latitude: location.coordinate.latitude,
            longitude: location.coordinate.longitude,
            accuracy: location.horizontalAccuracy,
            altitude: location.altitude,
            verticalAccuracy: location.verticalAccuracy,
            gpsLocationTypeId: locationTypeId
        )
        
        defer {
            // 3. Ensure the task ends even if the network call fails or succeeds
            if taskID != .invalid {
                UIApplication.shared.endBackgroundTask(taskID)
                taskID = .invalid
            }
        }
        
        do {
            let _: EmptyResponse = try await APIClient.shared.request(
                path: "GpsLocations/\(sessionId)",
                method: "POST",
                body: dto
            )
        } catch {
            print("❌ Failed to send background location: \(error)")
            throw error
        }
    }
}
// Empty response helper
struct EmptyResponse: Codable {}
