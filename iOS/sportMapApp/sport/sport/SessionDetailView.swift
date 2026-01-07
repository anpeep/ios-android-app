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
                    Map(position: $camera) {
                        // 1. Draw the Route Line (The path you traveled)
                        if locations.count > 1 {
                            // Filter out markers if you only want the line to connect actual path points
                            // or just map all locations if your backend includes coordinates for everything.
                            let pathCoords = locations.map {
                                CLLocationCoordinate2D(latitude: $0.latitude, longitude: $0.longitude)
                            }
                            MapPolyline(coordinates: pathCoords)
                                .stroke(.blue, lineWidth: 5)
                        }

                        // 2. Draw the Markers (Checkpoints and Waypoints)
                        ForEach(locations) { loc in
                            let coordinate = CLLocationCoordinate2D(latitude: loc.latitude, longitude: loc.longitude)
                            if let cpTypeId, loc.gpsLocationTypeId == cpTypeId {
                                Annotation("CP", coordinate: coordinate, anchor: .bottom) {
                                    Image("location")
                                        .resizable()
                                        .frame(width: 32, height: 32)
                                }
                            }
                            if let wpTypeId, loc.gpsLocationTypeId == wpTypeId {
                                Annotation("WP", coordinate: coordinate, anchor: .bottom) {
                                    Image("right-way")
                                        .resizable()
                                        .frame(width: 32, height: 32)
                                }
                            }
                        }

                        // 3. Start & End Markers
                        if let first = locations.first, let last = locations.last {
                            Marker("Start", coordinate: CLLocationCoordinate2D(latitude: first.latitude, longitude: first.longitude)).tint(.green)
                            Marker("End", coordinate: CLLocationCoordinate2D(latitude: last.latitude, longitude: last.longitude)).tint(.red)
                        }
                    
                    }
                    .overlay(alignment: .top) {
                        topBar
                    }
                    
                    // 📊 STATS
                    statsPanel
                }
            }
        }
        .navigationBarBackButtonHidden(true)
        .task {
            await load()

        }
    }
    
    // MARK: - UI
    
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
                stat("Avg Pace", formatPace(Int(session?.paceMin ?? 0)))            }
            
            HStack {
                stat("Speed", String(format: "%.1f km/h", session?.speed ?? 0))
                stat("Climb", String(format: "%.0f m", session?.climb ?? 0))
                stat("Descent", String(format: "%.0f m", session?.descent ?? 0))
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

            // ✅ 1. Load location types (CP / WP / LOC)
            let types = try await api.getLocationTypes()
            cpTypeId = types.first(where: { $0.name == "CP" })?.id
            wpTypeId = types.first(where: { $0.name == "WP" })?.id
            locTypeId = types.first(where: { $0.name == "LOC" })?.id

            print("🧭 Type IDs:")
            print("CP =", cpTypeId ?? "nil")
            print("WP =", wpTypeId ?? "nil")
            print("LOC =", locTypeId ?? "nil")

            // ✅ 2. Load session + locations
            async let s = api.getSession(id: sessionId)
            async let l = api.getSessionLocations(sessionId: sessionId)

            let loadedSession = try await s
            let loadedLocations = try await l

            await MainActor.run {
                self.session = loadedSession
                self.locations = loadedLocations.sorted { $0.recordedAt < $1.recordedAt }

                // 🔍 Debug: print each location type
                for loc in self.locations {
                    print("📍 Location type:", loc.gpsLocationTypeId)
                }

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
    
    func formatPace(_ secPerKm: Int) -> String {
        let m = secPerKm / 60
        let s = secPerKm % 60
        return "\(m):\(String(format: "%02d", s)) /km"
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
