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
import CoreData
@MainActor
final class LocationManager: NSObject, ObservableObject, CLLocationManagerDelegate {
    @Published var lastLocation: CLLocation?
    @Published var route: [RoutePoint] = []
        @Published var distance: Double = 0
        @Published var keepCentered: Bool = true
        @Published var isTracking: Bool = false
        @Published var authorizationStatus: CLAuthorizationStatus
    @Published var fastPaceThreshold: Double = 180 // 3 min/km in seconds
    @Published var slowPaceThreshold: Double = 1200 // 20 min/km in seconds
        // Checkpoint Stats
        @Published var directDistanceFromCheckpoint: Double = 0.0
        @Published var pathDistanceFromCheckpoint: Double = 0.0
        @Published var checkpoints: [CLLocationCoordinate2D] = []
    @Published var currentSessionId: String? = UUID().uuidString
        // Waypoint Stats
        @Published var directDistanceFromWaypoint: Double = 0.0
        @Published var pathDistanceFromWaypoint: Double = 0.0
        @Published var waypoint: CLLocationCoordinate2D?
        
        // Internal Logic References
        var lastCheckpointLocation: CLLocation?
        var lastWaypointLocation: CLLocation?
        var lastCheckpointTime: Date?
        var lastWaypointTime: Date?
        private let gpsService = GpsApiService()
        private let manager = CLLocationManager()
        var currentActivity: Activity<TrackingAttributes>?
        @Published var sessionStartTime: Date?
        @Published var elapsedTime: TimeInterval = 0
        private var durationTimer: Timer?
    @Published var degrees: Double = 0
        @Published var isCompassEnabled: Bool = false {
            didSet {
                if isCompassEnabled {
                    manager.startUpdatingHeading()
                } else {
                    manager.stopUpdatingHeading()
                }
            }
        }
    override init() {
        // Initialize status before super.init
        self.authorizationStatus = CLLocationManager().authorizationStatus
        super.init()
        
        manager.delegate = self
        manager.headingFilter = 1 // Only update if it moves by 1 degree
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
    // Inside LocationManager
    @Published var updateInterval: Double = 1.0 {
        didSet {
            // Higher interval = less accuracy needed to save battery/data
            manager.distanceFilter = updateInterval * 2
        }
    }
    // MARK: - Control Methods
    func requestPermission() {
        manager.requestAlwaysAuthorization()
    }
    func color(for pace: Double) -> Color {
        let pacePerKm = pace * 1000 // Convert sec/m to sec/km
        
        if pacePerKm <= fastPaceThreshold { return .green }
        if pacePerKm >= slowPaceThreshold { return .red }
        
        // Calculate gradient for middle ground (Yellow)
        let range = slowPaceThreshold - fastPaceThreshold
        let position = (pacePerKm - fastPaceThreshold) / range
        
        if position < 0.5 {
            return Color(red: position * 2, green: 1, blue: 0) // Green to Yellow
        } else {
            return Color(red: 1, green: 1 - ((position - 0.5) * 2), blue: 0) // Yellow to Red
        }
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
        var calculatedPace: Double = 0
        if let last = self.lastLocation {
            let distanceDelta = newLoc.distance(from: last)
            let timeDelta = newLoc.timestamp.timeIntervalSince(last.timestamp)
            
            if distanceDelta > 0.5 && timeDelta > 0 { // Small distance threshold to avoid infinity pace
                calculatedPace = timeDelta / distanceDelta // seconds per meter
            }
        }
        self.saveAndSync(location: newLoc, pace: calculatedPace)
        DispatchQueue.main.async {
            if let last = self.lastLocation {
                let delta = newLoc.distance(from: last)
                
                if delta < 100 { // Filter GPS jumps
                    self.distance += delta
                    self.route.append(RoutePoint(coordinate: newLoc.coordinate, pace: calculatedPace))
                    if self.lastCheckpointLocation != nil { self.pathDistanceFromCheckpoint += delta }
                    if self.lastWaypointLocation != nil { self.pathDistanceFromWaypoint += delta }
                    
                    self.updateLiveActivityUI()
                }
            } else {
                self.route.append(RoutePoint(coordinate: newLoc.coordinate, pace: calculatedPace))

            }
            
            self.lastLocation = newLoc
            
            // Distance calculations for Checkpoints/Waypoints
            if let cpLoc = self.lastCheckpointLocation {
                self.directDistanceFromCheckpoint = newLoc.distance(from: cpLoc)
            }
            if let wpLoc = self.lastWaypointLocation {
                self.directDistanceFromWaypoint = newLoc.distance(from: wpLoc)
            }
        }
    }
    func saveAndSync(location: CLLocation, pace: Double) {
        let context = PersistenceController.shared.container.viewContext
        let newLoc = SavedLocation(context: context)
        newLoc.lat = location.coordinate.latitude
        newLoc.lon = location.coordinate.longitude
        newLoc.timestamp = location.timestamp
        newLoc.pace = pace // Now saved for history!
        newLoc.isSynced = false
        newLoc.sessionId = self.currentSessionId
        
        do {
            try context.save()
            syncPendingLocations()
        } catch {
            print("❌ Core Data Save Error: \(error)")
        }
    }
    func syncPendingLocations() {
        let context = PersistenceController.shared.container.viewContext
        let request = NSFetchRequest<SavedLocation>(entityName: "SavedLocation")
        request.predicate = NSPredicate(format: "isSynced == false")
        request.sortDescriptors = [NSSortDescriptor(key: "timestamp", ascending: true)]

        guard let pending = try? context.fetch(request), !pending.isEmpty else { return }

        Task {
            for loc in pending {
                do {
                    // 1. Reconstruct the CLLocation object from Core Data values
                    let coordinate = CLLocationCoordinate2D(latitude: loc.lat, longitude: loc.lon)
                    let clLocation = CLLocation(
                        coordinate: coordinate,
                        altitude: 0,
                        horizontalAccuracy: 0,
                        verticalAccuracy: 0,
                        timestamp: loc.timestamp ?? Date()
                    )
                    
                    // 2. Extract the sessionId and provide a fallback for locationTypeId
                    let sessionId = loc.sessionId ?? ""
                    let typeId = "00000000-0000-0000-0000-000000000001" // Use your default workout type ID here

                    // 3. Call the function with the correct arguments and labels
                    try await gpsService.sendLocation(
                        location: clLocation,
                        sessionId: sessionId,
                        locationTypeId: typeId
                    )
                    
                    // 4. Mark as synced and save
                    loc.isSynced = true
                    try? context.save()
                    
                } catch {
                    print("📡 Sync failed for point at \(loc.timestamp ?? Date()): \(error)")
                    break // Stop the loop if connection is still bad
                }
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
    func startHeadingUpdates() {
        manager.delegate = self
        manager.startUpdatingHeading()
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
struct RoutePoint: Identifiable {
    let id = UUID()
    let coordinate: CLLocationCoordinate2D
    let pace: Double
    var type: String? = nil
}
