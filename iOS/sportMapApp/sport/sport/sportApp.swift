//
//  sportApp.swift
//  sport
//
//  Created by Anne-Riin Peep on 05.01.2026.
//


import SwiftUI

@main
struct sportApp: App {
    @StateObject var locationManager = LocationManager()
    @StateObject var sessionManager = SessionManager()

    var body: some Scene {
        WindowGroup {
            TrackingView()
                .environmentObject(locationManager)
                .environmentObject(sessionManager)
        }
    }
}
