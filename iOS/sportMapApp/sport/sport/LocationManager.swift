//
//  LocationManager.swift
//  sport
//
//  Created by Anne-Riin Peep on 05.01.2026.
//
import Foundation
import CoreLocation
import Combine
import SwiftUI


final class LocationManager: NSObject, ObservableObject, CLLocationManagerDelegate {
    @Published var lastLocation: CLLocation?
    @Published var trail: [CLLocation] = []
    @Published var currentWP: CLLocation?
    @Published var keepCentered = true
    @Published var heading: CLHeading?

    private let manager = CLLocationManager()

    override init() {
        super.init()
        manager.delegate = self
        manager.desiredAccuracy = kCLLocationAccuracyBest
        manager.allowsBackgroundLocationUpdates = true
        manager.pausesLocationUpdatesAutomatically = false
        manager.headingFilter = 5
    }


    func requestPermission() {
        manager.requestAlwaysAuthorization()
        manager.startUpdatingLocation()
        manager.startUpdatingHeading()
    }

    func startTracking() {
        trail.removeAll()
        manager.startUpdatingLocation()
    }

    func stopTracking() {
        manager.stopUpdatingLocation()
    }

    func addWP() {
        currentWP = lastLocation
    }

    func locationManager(
        _ manager: CLLocationManager,
        didUpdateLocations locations: [CLLocation]
    ) {
        guard let newLoc = locations.last else { return }

        if let last = lastLocation {
            let d = newLoc.distance(from: last)
            let t = newLoc.timestamp.timeIntervalSince(last.timestamp)
            if t > 0 && d / t > 15 { return } // jump filter
        }

        lastLocation = newLoc
        trail.append(newLoc)
    }

    func locationManager(
        _ manager: CLLocationManager,
        didUpdateHeading newHeading: CLHeading
    ) {
        heading = newHeading
    }
}
