//
//  APIConfig.swift
//  sport
//
//  Created by Anne-Riin Peep on 05.01.2026.
//


// MARK: - APIConfig
import Foundation

enum APIConfig {
    static let baseURL = URL(string: "https://sportmap.akaver.com/api/v1.0/")!
}

// MARK: - APIClient
final class APIClient {
    static let shared = APIClient()
    private init() {}

    var token: String?

    func request<T: Decodable>(
        _ path: String,
        method: String = "GET",
        body: Encodable? = nil
    ) async throws -> T {
        var request = URLRequest(url: APIConfig.baseURL.appendingPathComponent(path))
        request.httpMethod = method
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        if let token {
            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }
        if let body {
            request.httpBody = try JSONEncoder().encode(AnyEncodable(body))
        }
        let (data, response) = try await URLSession.shared.data(for: request)
        guard let http = response as? HTTPURLResponse, http.statusCode < 300 else {
            throw URLError(.badServerResponse)
        }
        return try JSONDecoder().decode(T.self, from: data)
    }
}

// MARK: - AnyEncodable helper
struct AnyEncodable: Encodable {
    let value: Encodable
    init(_ value: Encodable) { self.value = value }
    func encode(to encoder: Encoder) throws {
        try value.encode(to: encoder)
    }
}

// MARK: - AuthService
struct LoginRequest: Codable {
    let email: String
    let password: String
}

struct AuthResponse: Codable {
    let token: String
    let status: String
    let firstName: String
    let lastName: String
}

final class AuthService {
    func login(email: String, password: String) async throws -> AuthResponse {
        let response: AuthResponse = try await APIClient.shared.request(
            "Account/Login",
            method: "POST",
            body: LoginRequest(email: email, password: password)
        )
        APIClient.shared.token = response.token
        return response
    }
}

// MARK: - SessionService
struct GpsSessionCreate: Codable {
    let name: String
    let description: String
    let gpsSessionTypeId: UUID
    let recordedAt: Date
    let paceMin: Int?
    let paceMax: Int?
}

struct GpsSessionListItem: Codable, Identifiable {
    let id: UUID
    let name: String
    let recordedAt: Date
    let distance: Double
    let duration: Double
    let gpsSessionType: String
    let gpsLocationsCount: Int
    let userFirstLastName: String
}

final class SessionService {
    func fetchSessions() async throws -> [GpsSessionListItem] {
        try await APIClient.shared.request("GpsSessions")
    }

    func createSession(_ dto: GpsSessionCreate) async throws -> GpsSessionListItem {
        try await APIClient.shared.request("GpsSessions", method: "POST", body: dto)
    }
}

// MARK: - LocationService
struct GpsLocationCreate: Codable {
    let recordedAt: Date
    let latitude: Double
    let longitude: Double
    let accuracy: Double
    let altitude: Double
    let verticalAccuracy: Double
    let gpsLocationTypeId: UUID
}

final class LocationService {
    func postLocation(_ location: GpsLocationCreate, sessionId: UUID) async throws {
        let _: EmptyResponse = try await APIClient.shared.request(
            "GpsLocations/\(sessionId.uuidString)",
            method: "POST",
            body: location
        )
    }
}

struct EmptyResponse: Decodable {}

// MARK: - LocationManager + MapKit
import CoreLocation
import MapKit
import Combine


final class TrackingLocationManager: NSObject, ObservableObject, CLLocationManagerDelegate {
    private let manager = CLLocationManager()
    private let locationService = LocationService()
    var sessionId: UUID?

    override init() {
        super.init()
        manager.delegate = self
        manager.activityType = .fitness
        manager.desiredAccuracy = kCLLocationAccuracyBest
    }

    func start() {
        manager.requestAlwaysAuthorization()
        manager.startUpdatingLocation()
    }

    func stop() {
        manager.stopUpdatingLocation()
    }

    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        guard let location = locations.last,
              let sessionId else { return }

        let dto = GpsLocationCreate(
            recordedAt: Date(),
            latitude: location.coordinate.latitude,
            longitude: location.coordinate.longitude,
            accuracy: location.horizontalAccuracy,
            altitude: location.altitude,
            verticalAccuracy: location.verticalAccuracy,
            gpsLocationTypeId: UUID() // LOC type id
        )

        Task {
            try? await locationService.postLocation(dto, sessionId: sessionId)
        }
    }
}

// MARK: - SwiftUI Map View
import SwiftUI

struct TrackingMapView: View {
    @StateObject var locationManager = TrackingLocationManager()

    var body: some View {
        Map(coordinateRegion: .constant(MKCoordinateRegion(
            center: CLLocationCoordinate2D(latitude: 59.437, longitude: 24.7536),
            span: MKCoordinateSpan(latitudeDelta: 0.01, longitudeDelta: 0.01)
        )), showsUserLocation: true)
            .onAppear { locationManager.start() }
            .onDisappear { locationManager.stop() }
    }
}
