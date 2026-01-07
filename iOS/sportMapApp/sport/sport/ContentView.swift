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
    @State private var showSettings = false
    var body: some View {
        Group { // Using a Group allows us to apply a modifier to whatever is inside
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
        // Correct placement of onReceive
        .onReceive(NotificationCenter.default.publisher(for: UIApplication.didBecomeActiveNotification)) { _ in
            locationVM.syncPendingLocations()
        }
    }
}
