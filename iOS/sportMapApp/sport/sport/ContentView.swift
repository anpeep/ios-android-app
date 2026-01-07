//
//  ContentView.swift
//  sport
//
//  Created by Anne-Riin Peep on 05.01.2026.
//

import SwiftUI
import CoreLocation

struct ContentView: View {
    @StateObject var locationVM = LocationManager()

    var body: some View {
        switch locationVM.authorizationStatus {
        case .notDetermined:
            RequestLocationView()
                .environmentObject(locationVM)

        case .restricted:
            ErrorView(errorText: "Location use is restricted.")

        case .denied:
            ErrorView(errorText: "Location access denied. Enable it in Settings.")

        case .authorizedAlways, .authorizedWhenInUse:
            TrackingView()
                .environmentObject(locationVM)

        default:
            Text("Unknown status")
        }
    }
}
