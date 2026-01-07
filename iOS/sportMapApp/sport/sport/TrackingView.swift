import SwiftUI
import MapKit
import Combine

struct TrackingView: View {
    @EnvironmentObject var locationVM: LocationManager
    @StateObject var trackingVM = TrackingViewModel()
    @Environment(\.scenePhase) private var scenePhase
    @State private var camera: MapCameraPosition = .userLocation(fallback: .automatic)

    var body: some View {
        GeometryReader { geometry in
            VStack(spacing: 0) {
                // ================= ROW 0: MAP (25% Height) =================
                Map(position: $camera, interactionModes: .all) {
                    if locationVM.route.count > 1 {
                        MapPolyline(coordinates: locationVM.route)
                            .stroke(.blue, lineWidth: 5)
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
                    
                    if let loc = locationVM.lastLocation {
                        Marker("You", coordinate: loc.coordinate)
                    }
                }
                .frame(height: geometry.size.height * 0.75)
                .mapControls {
                    MapUserLocationButton()
                    MapCompass()
                }
                
                // ================= ROW 1: FIXED BUTTONS (Static) =================
                // This stays at the top of the 75% section
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

                            // 2. STOP / RESUME Toggle Button
                            if locationVM.isTracking {
                                Button {
                                    locationVM.pauseTracking()
                                } label: {
                                    Text("STOP").bold().frame(maxWidth: .infinity)
                                }
                                .buttonStyle(.borderedProminent).tint(.red)
                            } else {
                                Button {
                                    // Only start if a session actually exists, else resume
                                    if locationVM.sessionStartTime == nil {
                                        trackingVM.startTracking(locationVM: locationVM)
                                    } else {
                                        locationVM.resumeTracking()
                                    }
                                } label: {
                                    Text(locationVM.sessionStartTime == nil ? "START" : "RESUME")
                                        .bold().frame(maxWidth: .infinity)
                                }
                                .buttonStyle(.borderedProminent).tint(.green)
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
                                // Optional: Make it look grayed out when disabled
                                .opacity(trackingVM.isTracking ? 1.0 : 0.5)
                        }
                        .disabled(!trackingVM.isTracking) // This disables the button if NOT tracking
                        
                        Spacer()
                        
                        Button { trackingVM.showLogs = true } label: {
                            Image("folder").resizable().frame(width: 32, height: 32)
                        }
                    }
                    .padding()
                    .background(Color(.systemBackground)) // Solid color so stats don't bleed through
                    
                    Divider() // Visual separator between fixed buttons and scrolling stats
                }
                .zIndex(1) // Ensures the control bar stays on top of the scrolling content
                
                // ================= ROWS 2-4: SCROLLABLE STATS =================
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
                                // USE pathDistanceFromCheckpoint HERE:
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
                                // USE pathDistanceFromWaypoint HERE:
                                "From WP: \(locationVM.pathDistanceFromWaypoint, default: "%.0f") m",
                                "Direct line: \(locationVM.directDistanceFromWaypoint, default: "%.0f") m",
                                "Pace WP: \(formatPaceFromRaw(locationVM.paceSinceWaypoint)) min/km"
                            ]
                        )
                    }
                    .padding()
                }
                .frame(height: geometry.size.height * (0.25) - 30)
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

