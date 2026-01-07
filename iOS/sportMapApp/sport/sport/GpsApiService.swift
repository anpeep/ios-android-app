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
    func getMySessions(email: String) async throws -> [GpsSessionListItem] {
        // Wrap your parameters in URLQueryItem objects
        let queryItems = [
            URLQueryItem(name: "userEmails", value: email)
        ]

        return try await APIClient.shared.request(
            path: "GpsSessions",
            method: "GET",
            queryItems: queryItems // Now this matches the [URLQueryItem] type
        )
    }

    func getSessionDetails(id: String) async throws -> GpsSessionListItem {
        try await APIClient.shared.request(
            path: "GpsSessions/\(id)",
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
        
        return session
    }
    func updateSession(
        sessionId: String,
        name: String,
        description: String,
        sessionTypeId: String,
    ) async throws {
        
        // Create a concrete struct instead of [String: Any]
        let dto = UpdateSessionRequest(
            id: sessionId,
            name: name,
            description: description,
            recordedAt: ISO8601DateFormatter().string(from: Date()),
            paceMin: 0.0,
            paceMax: 0.0,
            gpsSessionTypeId: sessionTypeId
        )

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
    
    // MARK: - Send Location
    func sendLocation(
        location: CLLocation,
        sessionId: String,
        locationTypeId: String
    ) async throws {
        // 1. Tell iOS we are starting a background task
        let taskID = UIApplication.shared.beginBackgroundTask(withName: "SendLocation") {
            // This is called if we run out of time (usually after 30sec-3min)
            UIApplication.shared.endBackgroundTask(UIBackgroundTaskIdentifier.invalid)
        }

        // 2. Prepare the data
        let dto = CreateGpsLocationRequest(
            recordedAt: ISO8601DateFormatter().string(from: Date()),
            latitude: location.coordinate.latitude,
            longitude: location.coordinate.longitude,
            accuracy: location.horizontalAccuracy,
            altitude: location.altitude,
            verticalAccuracy: location.verticalAccuracy,
            gpsLocationTypeId: locationTypeId
        )

        do {
            let _: EmptyResponse = try await APIClient.shared.request(
                path: "GpsLocations/\(sessionId)",
                method: "POST",
                body: dto
            )
            print("🚀 Background location sent successfully")
        } catch {
            print("❌ Background send failed: \(error)")
        }
        UIApplication.shared.endBackgroundTask(taskID)
    }
}
// Empty response helper
struct EmptyResponse: Codable {}
