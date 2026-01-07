//
//  GpsModels.swift
//  sport
//
//  Created by Anne-Riin Peep on 06.01.2026.
//
import Foundation

// MARK: - Session Types

struct GpsSessionType: Codable, Identifiable {
    let id: String
    let name: String
    let description: String
    let paceMin: Double?
    let paceMax: Double?
}

// MARK: - Location Types

struct GpsLocationType: Codable, Identifiable {
    let id: String
    let name: String
    let description: String
}

// MARK: - Create Session

struct CreateGpsSessionRequest: Codable {
    let name: String
    let description: String
    let gpsSessionTypeId: String
    let recordedAt: String
    let paceMin: Double?
    let paceMax: Double?
}


struct GpsSessionResponse: Codable {
    let id: String
    let name: String
    let description: String
    let recordedAt: String
    let paceMin: Double?
    let paceMax: Double?
    let gpsSessionTypeId: String
}

struct CreateGpsLocationRequest: Codable {
    let recordedAt: String
    let latitude: Double
    let longitude: Double
    let accuracy: Double
    let altitude: Double
    let verticalAccuracy: Double
    let gpsLocationTypeId: String
}

struct GpsLocationDTO: Codable, Identifiable {
    let id: String
    let recordedAt: Date
    let latitude: Double
    let longitude: Double
    let accuracy: Double
    let altitude: Double
    let verticalAccuracy: Double
    let appUserId: String
    let gpsSessionId: String
    let gpsLocationTypeId: String
}

struct GpsSessionListItem: Codable, Identifiable {
    let id: String
    let name: String?
    let description: String?
    let recordedAt: String?
    let duration: Double
    let speed: Double
    let distance: Double
    let climb: Double
    let descent: Double
    let paceMin: Double?
    let paceMax: Double?
    let gpsSessionType: String
    let gpsLocationsCount: Int
    let userFirstLastName: String
}

