import SwiftUI
import MapKit
import Combine

struct TrackingView: View {
    @EnvironmentObject var locationVM: LocationManager
    @StateObject var trackingVM = TrackingViewModel()
    @Environment(\.scenePhase) private var scenePhase
    
    // Only use ONE camera state
    @State private var camera: MapCameraPosition = .userLocation(fallback: .automatic)
    @State private var showCompassOptions = true
    var mapView: some View {
        Map(position: $camera, interactionModes: .all) {
            if locationVM.route.count > 1 {
                MapPolyline(coordinates: locationVM.route.map { $0.coordinate })
                    .stroke(
                        .linearGradient(
                            stops: locationVM.route.map { point in
                                Gradient.Stop(
                                    color: locationVM.color(for: point.pace),
                                    location: CGFloat(locationVM.route.firstIndex(where: { $0.id == point.id }) ?? 0)
                                    / CGFloat(max(locationVM.route.count, 1))
                                )
                            },
                            startPoint: .init(x: 0, y: 0),
                            endPoint: .init(x: 1, y: 1)
                        ),
                        lineWidth: 5
                    )
            }
            
            ForEach(Array(locationVM.checkpoints.enumerated()), id: \.offset) { index, coordinate in
                Annotation("CP \(index + 1)", coordinate: coordinate, anchor: .bottom) {
                    Image("location").resizable().frame(width: 32, height: 32)
                }
            }
            
            if let wp = locationVM.waypoint {
                Annotation("Waypoint", coordinate: wp, anchor: .bottom) {
                    Image("right-way").resizable().frame(width: 32, height: 32)
                }
            }
        }
        .mapControls {
            MapUserLocationButton()
            MapCompass()
        }
        .overlay(alignment: .topLeading) {
            Button {
                trackingVM.showSettings = true
            } label: {
                Image("setting")
                    .resizable()
                    .scaledToFit()
                    .frame(width: 30, height: 30)
                    .padding(10)
                    .background(.ultraThinMaterial)
                    .clipShape(Circle())
                    .shadow(radius: 4)
            }
            .padding(.leading, 12)
            .padding(.top, 60)
        }
        .overlay(alignment: .topTrailing) {
            VStack(alignment: .trailing, spacing: 10) {
                Button {
                    withAnimation(.spring()) { showCompassOptions.toggle() }
                } label: {
                    Image(showCompassOptions ? "two-eyelashes" : "cartoon-eyes")
                        .resizable()
                        .frame(width: 30, height: 30)
                        .padding(8)
                        .background(.ultraThinMaterial)
                        .clipShape(Circle())
                }
                
                if showCompassOptions {
                    VStack(spacing: 8) {
                        CompassOptionButton(icon: "point") { camera = .userLocation(fallback: .automatic) }
                        CompassOptionButton(icon: "cardinal-point") { camera = .userLocation(fallback: .automatic) }
                        CompassOptionButton(icon: "point") { camera = .userLocation(fallback: .automatic) }
                        CompassOptionButton(icon: "tap") { camera = .automatic }
                    }
                    
                    RealCompassView(degrees: locationVM.degrees)
                        .frame(width: 80, height: 80)
                        .padding(.top, 10)
                        .transition(.move(edge: .trailing).combined(with: .opacity))
                }
            }
            .padding(.trailing, 12)
            .padding(.top, 60)
        }
    }
    var controlAndStatsPanel: some View {
        VStack(spacing: 0) {

            // ===== CONTROL BAR =====
            VStack(spacing: 0) {
                HStack {
                    Button {
                        locationVM.addCheckpoint()
                        trackingVM.sendCheckpoint(locationVM: locationVM)
                    } label: {
                        Image("location").resizable().frame(width: 30, height: 30)
                    }
                    .disabled(!locationVM.isTracking)
                    .opacity(locationVM.isTracking ? 1.0 : 0.5)

                    Spacer()

                    if locationVM.isTracking {
                        Button {
                            locationVM.pauseTracking()
                        } label: {
                            Text("STOP").bold().frame(maxWidth: .infinity)
                        }
                        .buttonStyle(.borderedProminent)
                        .tint(.red)
                    } else {
                        Button {
                            if locationVM.sessionStartTime == nil {
                                trackingVM.startTracking(locationVM: locationVM)
                            } else {
                                locationVM.resumeTracking()
                            }
                        } label: {
                            Text(locationVM.sessionStartTime == nil ? "START" : "RESUME")
                                .bold()
                                .frame(maxWidth: .infinity)
                        }
                        .buttonStyle(.borderedProminent)
                        .tint(.green)
                    }

                    Spacer()

                    Button { locationVM.addWaypoint() } label: {
                        Image("right-way").resizable().frame(width: 30, height: 30)
                    }

                    Spacer()

                    Button {
                        trackingVM.showEndSessionDialog = true
                    } label: {
                        Image("end")
                            .resizable()
                            .frame(width: 32, height: 32)
                            .opacity(trackingVM.isTracking ? 1.0 : 0.5)
                    }
                    .disabled(!trackingVM.isTracking)

                    Spacer()

                    Button { trackingVM.showLogs = true } label: {
                        Image("folder").resizable().frame(width: 32, height: 32)
                    }
                }
                .padding()
                .background(Color(.systemBackground))

                Divider()
            }

            // ===== STATS =====
            ScrollView {
                VStack(spacing: 8) {
                    statBlock(
                        title: "Session",
                        icon: "⏱",
                        color: .gray,
                        items: [
                            "Duration: \(formatTime(locationVM.elapsedTime))",
                            "Distance: \(locationVM.distance, default: "%.0f") m",
                            "Avg pace: \(formatPace(distance: locationVM.distance, time: locationVM.elapsedTime))"
                        ]
                    )

                    statBlock(
                        title: "Checkpoint",
                        icon: "🏁",
                        color: .blue,
                        items: [
                            "From CP: \(locationVM.pathDistanceFromCheckpoint, default: "%.0f") m",
                            "Direct line: \(locationVM.directDistanceFromCheckpoint, default: "%.0f") m",
                            "Pace CP: \(formatPaceFromRaw(locationVM.paceSinceCheckpoint)) min/km"
                        ]
                    )

                    statBlock(
                        title: "Waypoint",
                        icon: "📍",
                        color: .orange,
                        items: [
                            "From WP: \(locationVM.pathDistanceFromWaypoint, default: "%.0f") m",
                            "Direct line: \(locationVM.directDistanceFromWaypoint, default: "%.0f") m",
                            "Pace WP: \(formatPaceFromRaw(locationVM.paceSinceWaypoint)) min/km"
                        ]
                    )
                }
                .padding()
            }
        }
    }

    var body: some View {
        GeometryReader { geometry in
            let isLandscape = geometry.size.width > geometry.size.height

            if isLandscape {
                HStack(spacing: 0) {

                    // LEFT SIDE: PANEL
                    controlAndStatsPanel
                        .frame(width: geometry.size.width * 0.35)

                    // RIGHT SIDE: MAP
                    mapView
                        .frame(width: geometry.size.width * 0.65)
                }
            } else {
                VStack(spacing: 0) {

                    // TOP: MAP
                    mapView
                        .frame(height: geometry.size.height * 0.75)

                    // BOTTOM: PANEL
                    controlAndStatsPanel
                        .frame(height: geometry.size.height * 0.25)
                }
            }


        }
        .edgesIgnoringSafeArea(.top)
        .sheet(isPresented: $trackingVM.showEndSessionDialog) {
            VStack(spacing: 20) {
                Text("Finish Session").font(.title2)
                TextField("Session name", text: $trackingVM.sessionName).textFieldStyle(.roundedBorder)
                TextField("Description", text: $trackingVM.sessionDescription).textFieldStyle(.roundedBorder)
                Button("Save & End") { trackingVM.finishSession(locationVM: locationVM) }.buttonStyle(.borderedProminent)
                Button("Cancel") { trackingVM.showEndSessionDialog = false }
            }.padding()
        }
        .sheet(isPresented: $trackingVM.showLogs) { SessionListView() }
        .onChange(of: scenePhase) { oldPhase, newPhase in
            if newPhase == .active {
                Task {
                    try? await Task.sleep(nanoseconds: 800_000_000)
                    locationVM.refreshLocation()
                    locationVM.checkSharedActions()
                }
            }
        }
        .sheet(isPresented: $trackingVM.showLogs) {
            SessionListView()
        }
        .onAppear {
            locationVM.refreshLocation()
            locationVM.checkSharedActions()
        }
        .onReceive(Timer.publish(every: 2, on: .main, in: .common).autoconnect()) { _ in
            locationVM.checkSharedActions()
        }
        .sheet(isPresented: $trackingVM.showSettings) {
            SettingsView(isLiveMode: true).environmentObject(locationVM)
        }
    }
    @ViewBuilder
        func statBlock(title: String, icon: String, color: Color, items: [String]) -> some View {
            VStack(alignment: .leading, spacing: 4) {
                Text("\(icon) \(title)").font(.headline).foregroundColor(color)
                ForEach(items, id: \.self) { item in
                    Text(item)
                }
            }
            .padding()
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(color.opacity(0.1))
            .cornerRadius(12)
        }
    func formatTime(_ t: TimeInterval) -> String {
            let h = Int(t) / 3600
            let m = (Int(t) % 3600) / 60
            let s = Int(t) % 60
            return String(format: "%02d:%02d:%02d", h, m, s)
        }
        
        func formatPace(distance: Double, time: TimeInterval) -> String {
            guard distance > 5, time > 0 else { return "0:00" }
            let secondsPerKm = time / (distance / 1000)
            let min = Int(secondsPerKm) / 60
            let sec = Int(secondsPerKm) % 60
            return String(format: "%d:%02d min/km", min, sec)
        }
        
        func formatPaceFromRaw(_ secondsPerMeter: Double?) -> String {
            guard let sPerM = secondsPerMeter, sPerM > 0, sPerM < 100 else { return "0:00" }
            let totalSecondsPerKm = sPerM * 1000
            let minutes = Int(totalSecondsPerKm) / 60
            let seconds = Int(totalSecondsPerKm) % 60
            return String(format: "%d:%02d", minutes, seconds)
        }
    }

struct CompassOptionButton: View {
    let icon: String
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
            Image(icon)
                .resizable()
                .scaledToFit()
                .frame(width: 25, height: 25)
                .padding(10)
                .background(.white)
                .clipShape(Circle())
                .shadow(radius: 2)
        }
    }
}
