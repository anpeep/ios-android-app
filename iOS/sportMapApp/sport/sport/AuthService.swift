//
//  AuthService.swift
//  sport
//
//  Created by Anne-Riin Peep on 06.01.2026.
//

import Foundation

final class AuthService {
    func login(email: String, password: String) async throws -> AuthResponse {
        let dto = LoginRequest(email: email, password: password)
        
        // Pass the dto directly—APIClient handles the encoding
        let response: AuthResponse = try await APIClient.shared.request(
            path: "account/login",
            method: "POST",
            body: dto
        )

        // Setting this updates sharedDefaults automatically via the didSet in APIClient
        APIClient.shared.token = response.token
        UserDefaults.standard.set(email, forKey: "user_email")

        return response
    }

    func register(email: String, password: String, firstName: String, lastName: String) async throws -> AuthResponse {
        let dto = RegisterRequest(
            email: email,
            password: password,
            firstName: firstName,
            lastName: lastName
        )
        
        let response: AuthResponse = try await APIClient.shared.request(
            path: "account/register",
            method: "POST",
            body: dto
        )

        APIClient.shared.token = response.token
        return response
    }
}
