//
//  SessionDetailView.swift
//  sport
//
//  Created by Anne-Riin Peep on 06.01.2026.
//
import SwiftUI
import Foundation
import CoreLocation
import Combine
import MapKit

struct SessionDetailView: View {
    let sessionId: String
    @EnvironmentObject var locationVM: LocationManager
    @State private var showHistorySettings = false
    @Environment(\.dismiss) var dismiss
    
    @State private var session: GpsSessionListItem?
    @State private var locations: [GpsLocationDTO] = []
    @State private var isLoading = true
    
    @State private var camera: MapCameraPosition = .automatic
    @State private var cpTypeId: String?
    @State private var wpTypeId: String?
    @State private var locTypeId: String?
    var body: some View {
        ZStack {
            if isLoading {
                ProgressView("Loading session...")
            } else {
                VStack(spacing: 0) {
                    // 1. We wrap everything in a ZStack so buttons float over the map
                    ZStack(alignment: .topTrailing) {
                        
                        // The MapKitMapView now handles lines, colors, and markers
                        MapKitMapView(locationManager: locationVM)
                            .edgesIgnoringSafeArea(.top)
                        
                        // Floating Settings Button
                        Button {
                            showHistorySettings = true
                        } label: {
                            Label("Colors", systemImage: "paintpalette.fill")
                                .padding(10)
                                .background(.ultraThinMaterial, in: Capsule())
                                .shadow(radius: 2)
                        }
                        .padding(.trailing, 16)
                        .padding(.top, 80) // Pushed down to avoid the Back Button
                        
                        // Your existing topBar overlay (Back/Export)
                        topBar
                    }
                    
                    // 📊 STATS PANEL at the bottom
                    statsPanel
                }
            }
        }
        .navigationBarBackButtonHidden(true) // This now correctly attaches to the ZStack
            .sheet(isPresented: $showHistorySettings) {
                NavigationStack { // Use NavigationStack if on iOS 16+
                    SettingsView(isLiveMode: false)
                        .environmentObject(locationVM)
                }
            }
            .task {
                await load()
            }
    }
    
    var topBar: some View {
        HStack {
            Button {
                dismiss()
            } label: {
                Image(systemName: "chevron.left")
                    .font(.title2)
                    .padding(10)
                    .background(.ultraThinMaterial, in: Circle())
            }
            
            Spacer()
            
            Button {
                exportGPX()
            } label: {
                Image(systemName: "square.and.arrow.up")
                    .font(.title2)
                    .padding(10)
                    .background(.ultraThinMaterial, in: Circle())
            }
        }
        .padding()
    }
    
    var statsPanel: some View {
        VStack(spacing: 12) {
            HStack {
                stat("Distance", "\(session?.distance ?? 0, default: "%.0f") m")
                stat("Duration", formatTime(session?.duration ?? 0))
                            
                            // Avg Pace (Calculate it yourself to be safe)
                            let paceString = formatPace(
                                distance: session?.distance ?? 0,
                                time: session?.duration ?? 0
                            )
                            stat("Avg Pace", paceString)
                                 }

        }
        .padding()
        .background(.ultraThinMaterial)
    }
    
    func stat(_ title: String, _ value: String) -> some View {
        VStack {
            Text(title).font(.caption).foregroundColor(.secondary)
            Text(value).font(.headline)
        }
        .frame(maxWidth: .infinity)
    }
    func load() async {
        do {
            let api = GpsApiService()

            // 1. Load types
            let types = try await api.getLocationTypes()
            cpTypeId = types.first(where: { $0.name == "CP" })?.id
            wpTypeId = types.first(where: { $0.name == "WP" })?.id
            locTypeId = types.first(where: { $0.name == "LOC" })?.id

            // 2. Load session + locations
            async let s = api.getSession(id: sessionId)
            async let l = api.getSessionLocations(sessionId: sessionId)

            let loadedSession = try await s
            let loadedLocations = try await l
            await MainActor.run {
                self.session = loadedSession
                let sortedLocs = loadedLocations.sorted { $0.recordedAt < $1.recordedAt }
                self.locations = sortedLocs

                var processedPoints: [RoutePoint] = []

                for i in 0..<sortedLocs.count {
                    let current = sortedLocs[i]
                    var calculatedPace: Double = 0
                    
                    // Calculate pace relative to the previous point
                    if i > 0 {
                        let previous = sortedLocs[i-1]
                        let currLoc = CLLocation(latitude: current.latitude, longitude: current.longitude)
                        let prevLoc = CLLocation(latitude: previous.latitude, longitude: previous.longitude)
                        
                        let distance = currLoc.distance(from: prevLoc) // meters
                        let time = current.recordedAt.timeIntervalSince(previous.recordedAt) // seconds
                        
                        if distance > 0.5 { // ignore tiny movements to avoid infinite pace
                            // pace = seconds per kilometer
                            calculatedPace = time / (distance / 1000)
                        }
                    }

                    // Determine marker type
                    var pointType: String? = nil
                    if current.gpsLocationTypeId == cpTypeId { pointType = "CP" }
                    else if current.gpsLocationTypeId == wpTypeId { pointType = "WP" }
                    
                    if i == 0 { pointType = "START" }
                    else if i == sortedLocs.count - 1 { pointType = "END" }

                    processedPoints.append(RoutePoint(
                        coordinate: CLLocationCoordinate2D(latitude: current.latitude, longitude: current.longitude),
                        pace: calculatedPace,
                        type: pointType
                    ))
                }

                locationVM.route = processedPoints
                zoomToRoute()
                isLoading = false
            }
        } catch {
            print("❌ Failed to load session:", error)
            isLoading = false
        }
    }
    
    func zoomToRoute() {
        guard !locations.isEmpty else { return }
        
        let coords = locations.map {
            CLLocationCoordinate2D(latitude: $0.latitude, longitude: $0.longitude)
        }
        
        let rect = coords.reduce(MKMapRect.null) { rect, coord in
            let p = MKMapPoint(coord)
            let r = MKMapRect(x: p.x, y: p.y, width: 0, height: 0)
            return rect.isNull ? r : rect.union(r)
        }
        
        camera = .rect(rect.insetBy(dx: -300, dy: -300))
    }
    
    // MARK: - Helpers
    
    func formatTime(_ seconds: Double) -> String {
        let h = Int(seconds) / 3600
        let m = (Int(seconds) % 3600) / 60
        let s = Int(seconds) % 60
        return String(format: "%02d:%02d:%02d", h, m, s)
    }
    
    func formatPace(distance: Double, time: Double) -> String {
        // Basic safety check to avoid division by zero
        guard distance > 10, time > 0 else { return "--:--" }
        
        let secondsPerKm = time / (distance / 1000)
        
        // Safety check for unrealistic paces (e.g., standing still)
        guard secondsPerKm < 3600 else { return "--:--" }
        
        let min = Int(secondsPerKm) / 60
        let sec = Int(secondsPerKm) % 60
        return String(format: "%d:%02d", min, sec)
    }
    func exportGPX() {
        let gpxString = GPXExporter.makeGPX(locations: locations)
        
        // Use Documents instead of Tmp for better File Provider compatibility
        guard let documentsURL = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first else { return }
        let url = documentsURL.appendingPathComponent("session.gpx")
        
        do {
            try gpxString.write(to: url, atomically: true, encoding: .utf8)
            
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) {
                // Passing the URL directly often fails in Simulator.
                // On a real device, this works better.
                let vc = UIActivityViewController(activityItems: [url], applicationActivities: nil)
                
                // iPad check (mandatory)
                if let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
                   let rootVC = windowScene.windows.first(where: { $0.isKeyWindow })?.rootViewController {
                    
                    let topVC = self.getTopViewController(from: rootVC)
                    
                    if let popover = vc.popoverPresentationController {
                        popover.sourceView = topVC.view
                        popover.sourceRect = CGRect(x: topVC.view.bounds.midX, y: topVC.view.bounds.midY, width: 0, height: 0)
                    }
                    
                    topVC.present(vc, animated: true)
                }
            }
        } catch {
            print("❌ Write error: \(error)")
        }
    }
    
    // Helper to find the actual visible screen
    func getTopViewController(from viewController: UIViewController) -> UIViewController {
        if let presented = viewController.presentedViewController {
            return getTopViewController(from: presented)
        }
        return viewController
    }
}
enum GPXExporter {
    static func makeGPX(locations: [GpsLocationDTO]) -> String {
        var gpx = """
        <?xml version="1.0" encoding="UTF-8"?>
        <gpx version="1.1" creator="SportMap">
        <trk><name>Session</name><trkseg>
        """

        let formatter = ISO8601DateFormatter()

        for loc in locations {
            gpx += """
            <trkpt lat="\(loc.latitude)" lon="\(loc.longitude)">
                <time>\(formatter.string(from: loc.recordedAt))</time>
            </trkpt>
            """
        }

        gpx += "</trkseg></trk></gpx>"
        return gpx
    }
}
