//
//  AccountViewModel.swift
//  sport
//
//  Created by Anne-Riin Peep on 06.01.2026.
//


import Foundation
import SwiftUI
import CoreLocation
import Combine

@MainActor
final class AccountViewModel: ObservableObject {
    @Published var isLoggedIn: Bool = false
    @Published var userName: String = ""

    private let authService = AuthService()

    init() {
        APIClient.shared.loadTokenFromStorage()
        if APIClient.shared.token != nil {
            isLoggedIn = true
        }
    }

    func login(email: String, password: String) async throws {
        let response = try await authService.login(email: email, password: password)
        userName = response.firstName!
        isLoggedIn = true
    }

    func register(email: String, password: String, firstName: String, lastName: String) async throws {
        let response = try await authService.register(
            email: email,
            password: password,
            firstName: firstName,
            lastName: lastName
        )
        userName = response.firstName!
        isLoggedIn = true
    }

    func logout() {
        APIClient.shared.token = nil
        isLoggedIn = false
    }
}
