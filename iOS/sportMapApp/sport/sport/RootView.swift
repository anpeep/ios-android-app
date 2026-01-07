//
//  RootView.swift
//  sport
//
//  Created by Anne-Riin Peep on 06.01.2026.
//


import SwiftUI
import CoreLocation
import Foundation

struct RootView: View {
    @EnvironmentObject var locationVM: LocationManager
    @EnvironmentObject var accountVM: AccountViewModel

    var body: some View {
        switch locationVM.authorizationStatus {
        case .notDetermined:
            RequestLocationView()

        case .denied, .restricted:
            ErrorView(errorText: "Location permission is required.")

        case .authorizedAlways, .authorizedWhenInUse:
            if accountVM.isLoggedIn {
                TrackingView()
            } else {
                LoginView()
            }

        default:
            Text("Unknown state")
        }
    }
}
