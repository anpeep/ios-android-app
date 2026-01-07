//
//  APIConfig.swift
//  sport
//
//  Created by Anne-Riin Peep on 05.01.2026.
//


import Foundation

final class APIClient {
    static let shared = APIClient()
    private init() {}

    // Ensure the base URL does NOT have a trailing slash if using appendingPathComponent
    private let baseURL = URL(string: "https://sportmap.akaver.com/api/v1.0")!

    var token: String? {
        didSet {
            if let token {
                UserDefaults.standard.set(token, forKey: "jwt_token")
            } else {
                UserDefaults.standard.removeObject(forKey: "jwt_token")
            }
        }
    }


    func loadTokenFromStorage() {
        token = UserDefaults.standard.string(forKey: "jwt_token")
    }

    private let encoder: JSONEncoder = {
        let e = JSONEncoder()
        e.dateEncodingStrategy = .iso8601
        return e
    }()

    private let decoder: JSONDecoder = {
        let d = JSONDecoder()
        d.dateDecodingStrategy = .iso8601
        return d
    }()

    // Updated request method
    func request<T: Decodable>(
        path: String,
        method: String = "GET",
        body: Encodable? = nil,
        queryItems: [URLQueryItem]? = nil
    ) async throws -> T {
        
        var components = URLComponents(url: baseURL.appendingPathComponent(path), resolvingAgainstBaseURL: false)
            
            // 2. Attach query items (e.g., ?email=test@test.com)
            if let queryItems = queryItems {
                components?.queryItems = queryItems
            }
            
            guard let fullURL = components?.url else {
                throw URLError(.badURL)
            }
        var request = URLRequest(url: fullURL)
        request.httpMethod = method
        
        // Set Required Headers
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue("application/json", forHTTPHeaderField: "Accept")

        // Include Token if available
        if let token = self.token {
            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }

        if let body {
            request.httpBody = try encoder.encode(AnyEncodable(body))
        }

        print("➡️ Requesting: \(method) \(fullURL.absoluteString)")

        let (data, response) = try await URLSession.shared.data(for: request)

        guard let http = response as? HTTPURLResponse else {
            throw URLError(.badServerResponse)
        }

        // Print status code for debugging
        print("⬅️ Response Code: \(http.statusCode)")

        if http.statusCode >= 300 {
            // Print error body from server if available
            if let errorString = String(data: data, encoding: .utf8) {
                print("❌ Server Error: \(errorString)")
            }
            throw URLError(.badServerResponse)
        }

        return try decoder.decode(T.self, from: data)
    }
    func requestVoid(
        path: String,
        method: String = "PUT",
        body: Encodable? = nil
    ) async throws {

        var request = URLRequest(url: baseURL.appendingPathComponent(path))
        request.httpMethod = method
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue("application/json", forHTTPHeaderField: "Accept")

        if let token {
            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }

        if let body {
            request.httpBody = try encoder.encode(AnyEncodable(body))
        }

        print("➡️ Requesting: \(method) \(request.url!.absoluteString)")

        let (_, response) = try await URLSession.shared.data(for: request)

        guard let http = response as? HTTPURLResponse, http.statusCode < 300 else {
            throw URLError(.badServerResponse)
        }

        print("✅ Void request success")
    }

}

// Ensure your LoginResponse matches the JSON structure you provided
struct LoginResponse: Codable {
    let token: String
    let status: String
    let firstName: String
    let lastName: String
}

// MARK: - AnyEncodable helper
struct AnyEncodable: Encodable {
    let value: Encodable
    init(_ value: Encodable) { self.value = value }
    func encode(to encoder: Encoder) throws {
        try value.encode(to: encoder)
    }
}
