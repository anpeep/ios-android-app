import SwiftUI
import MapKit
import Combine

struct TrackingView: View {
    @EnvironmentObject var locationVM: LocationManager
    @StateObject var trackingVM = TrackingViewModel()
    @Environment(\.scenePhase) private var scenePhase
    @State private var camera: MapCameraPosition = .userLocation(fallback: .automatic)
    @Environment(\.horizontalSizeClass) var hSize

    var body: some View {
        ZStack {
            // ================= MAP =================
            Map(position: $camera, interactionModes: .all) {
                if locationVM.route.count > 1 {
                    MapPolyline(coordinates: locationVM.route)
                        .stroke(.blue, lineWidth: 5)
                }

                ForEach(Array(locationVM.checkpoints.enumerated()), id: \.offset) { index, coordinate in
                    Annotation("CP \(index + 1)", coordinate: coordinate, anchor: .bottom) {
                        Image("location")
                            .resizable()
                            .frame(width: 32, height: 32)
                    }
                }

                if let wp = locationVM.waypoint {
                    Annotation("Waypoint", coordinate: wp, anchor: .bottom) {
                        Image("right-way")
                            .resizable()
                            .frame(width: 32, height: 32)
                    }
                }

                if let loc = locationVM.lastLocation {
                    Marker("You", coordinate: loc.coordinate)
                }
            }
            .mapControls {
                MapUserLocationButton()
                MapCompass()
            }

            // ================= STATS OVERLAY =================
            let isLandscape = hSize == .regular

            if isLandscape {
                HStack {
                    StatsPanel(vm: locationVM)
                        .frame(width: 260)
                        .padding()

                    Spacer()
                }
            } else {
                VStack {
                    Spacer()

                    StatsPanel(vm: locationVM)
                        .padding(.bottom, 110) // sits above buttons
                }
            }

            // ================= BOTTOM BAR =================
            VStack {
                Spacer()

                HStack {
                    Button {
                        locationVM.addCheckpoint()
                        trackingVM.sendCheckpoint(locationVM: locationVM)
                    } label: {
                        Image("location")
                            .resizable()
                            .frame(width: 32, height: 32)
                    }

                    Spacer()

                    if trackingVM.isTracking {
                        Button {
                            trackingVM.showEndSessionDialog = true
                        } label: {
                            Text("STOP")
                                .frame(maxWidth: .infinity)
                        }
                        .buttonStyle(.borderedProminent)
                        .tint(.red)
                    } else {
                        Button {
                            trackingVM.startTracking(locationVM: locationVM)
                        } label: {
                            Text("START NEW SESSION")
                                .frame(maxWidth: .infinity)
                        }
                        .buttonStyle(.borderedProminent)
                        .tint(.green)
                    }

                    Spacer()

                    Button {
                        locationVM.addWaypoint()
                    } label: {
                        Image("right-way")
                            .resizable()
                            .frame(width: 32, height: 32)
                    }

                    Spacer()

                    Button {
                        trackingVM.showEndSessionDialog = true
                    } label: {
                        Image("end")
                            .resizable()
                            .frame(width: 36, height: 36)
                    }

                    Spacer()

                    Button {
                        trackingVM.showLogs = true
                    } label: {
                        Image("folder")
                            .resizable()
                            .frame(width: 36, height: 36)
                    }
                }
                .padding()
                .background(.ultraThinMaterial)
            }
        

            .sheet(isPresented: $trackingVM.showEndSessionDialog) {
                VStack(spacing: 20) {
                    Text("Finish Session")
                        .font(.title2)

                    TextField("Session name", text: $trackingVM.sessionName)
                        .textFieldStyle(.roundedBorder)

                    TextField("Description", text: $trackingVM.sessionDescription)
                        .textFieldStyle(.roundedBorder)

                    Button("Save & End") {
                        trackingVM.finishSession(locationVM: locationVM)                    }
                    .buttonStyle(.borderedProminent)

                    Button("Cancel") {
                        trackingVM.showEndSessionDialog = false
                    }
                }
                .padding()
            }

        }
        .onChange(of: scenePhase) { newPhase in
            if newPhase == .active {
                print("🔄 App returned to foreground, syncing actions")
                locationVM.refreshLocation()
                locationVM.checkSharedActions()
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
}
