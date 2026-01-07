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
import ActivityKit

final class LocationManager: NSObject, ObservableObject, CLLocationManagerDelegate {
    // MARK: - Published Properties
    @Published var lastLocation: CLLocation?
    @Published var route: [CLLocationCoordinate2D] = []
    @Published var distance: Double = 0
    @Published var keepCentered: Bool = true
    @Published var isTracking: Bool = false
    @Published var authorizationStatus: CLAuthorizationStatus
    
    // Markers from ViewModel
    @Published var checkpoints: [CLLocationCoordinate2D] = []
    @Published var waypoint: CLLocationCoordinate2D?
    
    private let manager = CLLocationManager()
    var currentActivity: Activity<TrackingAttributes>?
    var sessionStartTime: Date?
    var lastCheckpointLocation: CLLocation?
    var lastWaypointLocation: CLLocation?
    var lastCheckpointTime: Date?
    var lastWaypointTime: Date?

    override init() {
        // Initialize status before super.init
        self.authorizationStatus = CLLocationManager().authorizationStatus
        super.init()
        
        manager.delegate = self
        manager.desiredAccuracy = kCLLocationAccuracyBest
        manager.activityType = .fitness
        manager.allowsBackgroundLocationUpdates = true
        manager.pausesLocationUpdatesAutomatically = false
        manager.showsBackgroundLocationIndicator = true
    }
    
    // MARK: - Control Methods
    func requestPermission() {
        manager.requestAlwaysAuthorization()
    }
    
    func startGPS() {
        manager.startUpdatingLocation()
        guard ActivityAuthorizationInfo().areActivitiesEnabled else { return }
        let attributes = TrackingAttributes(startTime: Date())
        let initialContentState = TrackingAttributes.ContentState(
            distanceCovered: 0.0,
            lastActionName: "Started"
        )
        do {
            let content = ActivityContent(state: initialContentState, staleDate: nil)
            
            currentActivity = try Activity<TrackingAttributes>.request(
                attributes: attributes,
                content: content
            )
            print("✅ Live Activity Started!")
        } catch {
            print("❌ Failed to start Live Activity: \(error.localizedDescription)")
        }
    }
    
    func stopGPS() {
        manager.stopUpdatingLocation()
    }
    
    func startTracking() {
        isTracking = true
        route.removeAll()
        distance = 0
        sessionStartTime = Date()
        lastCheckpointLocation = nil
        lastWaypointLocation = nil
        lastCheckpointTime = nil
        lastWaypointTime = nil
        startGPS()
    }

    
    func stopTracking() {
        isTracking = false
        stopGPS()
        checkpoints.removeAll()
        waypoint = nil
    }
    
    func addCheckpoint() {
        if let loc = lastLocation {
            checkpoints.append(loc.coordinate)
            lastCheckpointLocation = loc
            lastCheckpointTime = Date()
        }
    }
    func addWaypoint() {
        if let loc = lastLocation {
            waypoint = loc.coordinate
            lastWaypointLocation = loc
            lastWaypointTime = Date()
        }
    }
    var sessionDuration: TimeInterval {
        guard let start = sessionStartTime else { return 0 }
        return Date().timeIntervalSince(start)
    }

    var averagePace: Double {
        guard distance > 0 else { return 0 }
        return sessionDuration / distance  // sec per meter
    }

    var distanceFromCheckpoint: Double {
        guard let last = lastCheckpointLocation, let now = lastLocation else { return 0 }
        return now.distance(from: last)
    }

    var distanceFromWaypoint: Double {
        guard let last = lastWaypointLocation, let now = lastLocation else { return 0 }
        return now.distance(from: last)
    }

    var paceSinceCheckpoint: Double {
        guard let t = lastCheckpointTime else { return 0 }
        let dt = Date().timeIntervalSince(t)
        guard distanceFromCheckpoint > 0 else { return 0 }
        return dt / distanceFromCheckpoint
    }

    var paceSinceWaypoint: Double {
        guard let t = lastWaypointTime else { return 0 }
        let dt = Date().timeIntervalSince(t)
        guard distanceFromWaypoint > 0 else { return 0 }
        return dt / distanceFromWaypoint
    }

    func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        DispatchQueue.main.async {
            self.authorizationStatus = manager.authorizationStatus
        }
    }
    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        guard let newLoc = locations.last else { return }
        
        DispatchQueue.main.async {
            if let last = self.lastLocation {
                let delta = newLoc.distance(from: last)
                
                if delta < 100 {
                    self.distance += delta
                    self.route.append(newLoc.coordinate)
                    
                    // --- ADD THIS LINE TO UPDATE THE LIVE ACTIVITY ---
                    self.updateLiveActivityUI()
                }
            } else {
                self.route.append(newLoc.coordinate)
            }
            self.lastLocation = newLoc
        }
    }
    func checkSharedActions() {
        let shared = UserDefaults(suiteName: "group.taltech.anpeep.sport")
        
        guard let action = shared?.string(forKey: "pending_action") else { return }
        
        guard let coord = lastLocation?.coordinate else {
            print("⏳ Waiting for GPS fix before placing marker")
            return
        }

        DispatchQueue.main.async {
            if action == "CP" {
                self.checkpoints.append(coord)
                print("🏁 CP added from lockscreen at", coord)
            }

            if action == "WP" {
                self.waypoint = coord
                print("📍 WP added from lockscreen at", coord)
            }

            shared?.removeObject(forKey: "pending_action")
        }
    }
    func refreshLocation() {
        if isTracking {
            manager.startUpdatingLocation()
        } else {
            if let loc = manager.location {
                self.lastLocation = loc
            }
        }
    }

    
    // Add this helper function to your LocationManager
    func updateLiveActivityUI() {
        let updatedState = TrackingAttributes.ContentState(
            distanceCovered: self.distance,
            lastActionName: "Tracking..."
        )
        
        // Live Activity updates must be wrapped in a Task
        Task {
            await currentActivity?.update(using: updatedState)
        }
    }
    struct MapPoint: Identifiable {
        let id = UUID()
        let coordinate: CLLocationCoordinate2D
        let type: PointType
    }

    enum PointType {
        case checkpoint, waypoint
    }

    // Inside LocationManager
    @Published var mapPoints: [MapPoint] = []
}

