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
@MainActor
final class LocationManager: NSObject, ObservableObject, CLLocationManagerDelegate {
    @Published var lastLocation: CLLocation?
        @Published var route: [CLLocationCoordinate2D] = []
        @Published var distance: Double = 0
        @Published var keepCentered: Bool = true
        @Published var isTracking: Bool = false
        @Published var authorizationStatus: CLAuthorizationStatus
        
        // Checkpoint Stats
        @Published var directDistanceFromCheckpoint: Double = 0.0
        @Published var pathDistanceFromCheckpoint: Double = 0.0
        @Published var checkpoints: [CLLocationCoordinate2D] = []
        
        // Waypoint Stats
        @Published var directDistanceFromWaypoint: Double = 0.0
        @Published var pathDistanceFromWaypoint: Double = 0.0
        @Published var waypoint: CLLocationCoordinate2D?
        
        // Internal Logic References
        var lastCheckpointLocation: CLLocation?
        var lastWaypointLocation: CLLocation?
        var lastCheckpointTime: Date?
        var lastWaypointTime: Date?
        
        private let manager = CLLocationManager()
        var currentActivity: Activity<TrackingAttributes>?
        @Published var sessionStartTime: Date?
        @Published var elapsedTime: TimeInterval = 0
        private var durationTimer: Timer?

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
        Task {
            for activity in Activity<TrackingAttributes>.activities {
                // Use activity.content.state to end with the current values
                let finalContent = ActivityContent(state: activity.content.state, staleDate: nil)
                await activity.end(finalContent, dismissalPolicy: .immediate)            }
        }
    }
    
    // MARK: - Control Methods
    func requestPermission() {
        manager.requestAlwaysAuthorization()
    }
    
    func startGPS(startTime: Date) {
        manager.startUpdatingLocation()
        guard ActivityAuthorizationInfo().areActivitiesEnabled else { return }
        let attributes = TrackingAttributes(startTime: startTime)
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
            pathDistanceFromCheckpoint = 0
            pathDistanceFromWaypoint = 0
            directDistanceFromCheckpoint = 0
            directDistanceFromWaypoint = 0
            lastCheckpointLocation = nil
            lastWaypointLocation = nil
            
            let now = Date()
            sessionStartTime = now
            elapsedTime = 0
            startGPS(startTime: now)
            
            durationTimer?.invalidate()
            durationTimer = Timer.scheduledTimer(withTimeInterval: 1, repeats: true) { _ in
                Task { @MainActor in
                    guard let start = self.sessionStartTime else { return }
                    self.elapsedTime = Date().timeIntervalSince(start)
                }
            }
            RunLoop.main.add(durationTimer!, forMode: .common)
        }
    func pauseTracking() {
        isTracking = false
        manager.stopUpdatingLocation()
        durationTimer?.invalidate()
        durationTimer = nil
        
        // Update Live Activity to show Paused status
        updateLiveActivityUI(status: "Paused")
    }
    func resumeTracking() {
        isTracking = true
        manager.startUpdatingLocation()
        
        let now = Date()
        sessionStartTime = now.addingTimeInterval(-elapsedTime)
        
        durationTimer = Timer.scheduledTimer(withTimeInterval: 1, repeats: true) { _ in
            Task { @MainActor in
                guard let start = self.sessionStartTime else { return }
                self.elapsedTime = Date().timeIntervalSince(start)
            }
        }
        RunLoop.main.add(durationTimer!, forMode: .common)
        
        // Update Live Activity to show Tracking status
        updateLiveActivityUI(status: "Tracking...")
    }
    func stopTracking() {
        isTracking = false
        stopGPS()
        checkpoints.removeAll()
        route.removeAll()
        distance = 0
        pathDistanceFromCheckpoint = 0
        pathDistanceFromWaypoint = 0
        directDistanceFromCheckpoint = 0
        directDistanceFromWaypoint = 0
        
        // 3. IMPORTANT: Clear the REFERENCES (This stops the "Ghost" stats)
        lastCheckpointLocation = nil
        lastWaypointLocation = nil
        lastCheckpointTime = nil
        lastWaypointTime = nil
        sessionStartTime = nil
        elapsedTime = 0
        
        waypoint = nil
        durationTimer?.invalidate()
            durationTimer = nil
        let finalContentState = TrackingAttributes.ContentState(
                distanceCovered: self.distance,
                lastActionName: "Finished"
            )
            
            // 2. Tell iOS to stop the notification/timer
            Task {
                for activity in Activity<TrackingAttributes>.activities {
                    // .dismissalPolicy(.immediate) removes it from the lock screen instantly
                    await activity.end(
                        ActivityContent(state: finalContentState, staleDate: nil),
                        dismissalPolicy: .immediate
                    )
                }
                print("🛑 Live Activity Ended")
            }
        
    }
    func addCheckpoint() {
            if let loc = lastLocation {
                checkpoints.append(loc.coordinate)
                lastCheckpointLocation = loc
                pathDistanceFromCheckpoint = 0.0
                directDistanceFromCheckpoint = 0.0
                lastCheckpointTime = Date()
            }
        }

        func addWaypoint() {
            if let loc = lastLocation {
                waypoint = loc.coordinate
                lastWaypointLocation = loc
                pathDistanceFromWaypoint = 0.0
                directDistanceFromWaypoint = 0.0
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

    var paceSinceCheckpoint: Double {
        guard let t = lastCheckpointTime, pathDistanceFromCheckpoint > 0 else { return 0 }
        let dt = Date().timeIntervalSince(t)
        return dt / pathDistanceFromCheckpoint // dt is seconds, path is meters
    }

    var paceSinceWaypoint: Double {
        guard let t = lastWaypointTime, pathDistanceFromWaypoint > 0 else { return 0 }
        let dt = Date().timeIntervalSince(t)
        return dt / pathDistanceFromWaypoint
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
                
                if delta < 100 { // Filter GPS jumps
                    self.distance += delta
                    self.route.append(newLoc.coordinate)
                    
                    // Increment Path Distances (Winding path)
                    if self.lastCheckpointLocation != nil { self.pathDistanceFromCheckpoint += delta }
                    if self.lastWaypointLocation != nil { self.pathDistanceFromWaypoint += delta }
                    
                    self.updateLiveActivityUI()
                }
            } else {
                self.route.append(newLoc.coordinate)
            }
            
            self.lastLocation = newLoc
            
            // Calculate Direct Distances (Straight line / Displacement)
            if let cpLoc = self.lastCheckpointLocation {
                self.directDistanceFromCheckpoint = newLoc.distance(from: cpLoc)
            }
            if let wpLoc = self.lastWaypointLocation {
                self.directDistanceFromWaypoint = newLoc.distance(from: wpLoc)
            }
        }
    }
    func checkSharedActions() {
        let shared = UserDefaults(suiteName: "group.taltech.anpeep.sport")
        if shared?.bool(forKey: "action_consumed") == true { return }
        guard let action = shared?.string(forKey: "pending_action") else { return }
        guard let coord = lastLocation?.coordinate else {
            return
        }
        withAnimation(.none) {
                if action == "CP" {
                    checkpoints.append(coord)
                    lastCheckpointLocation = lastLocation
                    lastCheckpointTime = Date()
                    print("🏁 CP added from lockscreen at", coord)
                }

                if action == "WP" {
                    waypoint = coord
                    lastWaypointLocation = lastLocation
                    lastWaypointTime = Date()
                    print("📍 WP added from lockscreen at", coord)
                }
            }
        shared?.set(true, forKey: "action_consumed")

            shared?.removeObject(forKey: "pending_action")
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

    func updateLiveActivityUI(status: String = "Tracking...") {
        let updatedState = TrackingAttributes.ContentState(
            distanceCovered: self.distance,
            lastActionName: status
        )
        // Live Activity updates must be wrapped in a Task
        Task {
            let updatedContent = ActivityContent(state: updatedState, staleDate: nil)
            await currentActivity?.update(updatedContent)
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

