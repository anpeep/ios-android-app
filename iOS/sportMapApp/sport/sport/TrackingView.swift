//
//  TrackingView.swift
//  sport
//
//  Created by Anne-Riin Peep on 05.01.2026.
//

import SwiftUI
import MapKit

struct TrackingView: View {
    @EnvironmentObject var locationManager: LocationManager
    @EnvironmentObject var sessionManager: SessionManager

    var body: some View {
        ZStack(alignment: .bottom) {
            MapKitMapView(locationManager: locationManager)
                .edgesIgnoringSafeArea(.all)

            HStack {
                Button("Start") {
                    sessionManager.startSession()
                    locationManager.startTracking()
                }

                Button("CP") {
                    sessionManager.addCP(locationManager.lastLocation)
                }

                Button("WP") {
                    locationManager.addWP()
                    sessionManager.addWP(locationManager.lastLocation)
                }

                Button("Stop") {
                    locationManager.stopTracking()
                    sessionManager.stopSession()
                }
            }
            .padding()
            .background(.ultraThinMaterial)
        }
    }
}
