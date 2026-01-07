//
//  SettingsView.swift
//  sport
//
//  Created by Anne-Riin Peep on 07.01.2026.
//

import SwiftUI
import Foundation
import CoreLocation
import Combine
struct SettingsView: View {
    @EnvironmentObject var locationVM: LocationManager
    @Environment(\.dismiss) var dismiss
    
    // Add this property
    let isLiveMode: Bool

    var body: some View {
        // Remove the extra NavigationView here since you already wrap
        // SettingsView in a NavigationView/Stack inside the .sheet
        Form {
            // Only show GPS Interval during a live session
            if isLiveMode {
                Section(header: Text("GPS Updates")) {
                    HStack {
                        Text("Interval")
                        Spacer()
                        Text("\(Int(locationVM.updateInterval)) sec")
                    }
                    Slider(value: $locationVM.updateInterval, in: 1...10, step: 1)
                }
            }

            Section(header: Text("Pace Coloring (min/km)")) {
                // Convert seconds to readable minutes for the label
                Stepper("Fastest (Green): \(Int(locationVM.fastPaceThreshold / 60)) min/km",
                        value: $locationVM.fastPaceThreshold, in: 60...600, step: 30)
                
                Stepper("Slowest (Red): \(Int(locationVM.slowPaceThreshold / 60)) min/km",
                        value: $locationVM.slowPaceThreshold, in: 300...1800, step: 30)
            }
        }
        .navigationTitle("Settings")
        .toolbar {
            ToolbarItem(placement: .confirmationAction) {
                Button("Done") { dismiss() }
            }
        }
    }
}
