//
//  AuthModels.swift
//  sport
//
//  Created by Anne-Riin Peep on 06.01.2026.
//
import Foundation

struct AuthResponse: Codable {
    let token: String
    let status: String
    let firstName: String?
    let lastName: String?
}

struct LoginRequest: Encodable {
    let email: String
    let password: String
}

struct RegisterRequest: Encodable {
    let email: String
    let password: String
    let firstName: String
    let lastName: String
}
