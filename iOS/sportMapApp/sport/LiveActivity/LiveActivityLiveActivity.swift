import ActivityKit
import WidgetKit
import SwiftUI
import AppIntents // Required for Buttons


struct LiveActivityLiveActivity: Widget {
    var body: some WidgetConfiguration {
        ActivityConfiguration(for: TrackingAttributes.self) { context in
            // LOCK SCREEN UI
            VStack(spacing: 12) {
                HStack(alignment: .center) {
                    VStack(alignment: .leading) {
                        Text("Duration").font(.caption).opacity(0.8)
                        
                        if context.state.lastActionName == "Paused" {
                            // When paused, we show a static duration instead of a ticking timer
                            Text("PAUSED")
                                .font(.title2).bold()
                                .foregroundColor(.yellow)
                        } else {
                            Text(context.attributes.startTime, style: .timer)
                                .font(.title2).bold()
                                .foregroundColor(.white)
                        }
                    }
                    
                    Spacer()
                    
                    // Status indicator in the middle
                    VStack {
                         Text(context.state.lastActionName)
                            .font(.caption2).bold()
                            .padding(.horizontal, 8)
                            .padding(.vertical, 4)
                            .background(context.state.lastActionName == "Paused" ? Color.yellow : Color.green)
                            .cornerRadius(4)
                            .foregroundColor(.black)
                    }
                    
                    Spacer()
                    
                    VStack(alignment: .trailing) {
                        Text("Distance").font(.caption).opacity(0.8)
                        Text(String(format: "%.2f km", context.state.distanceCovered / 1000))
                            .font(.title2).bold()
                    }
                }
                
                Divider().background(Color.white.opacity(0.3))
                
                // ACTION BUTTONS
                HStack {
                    // We disable buttons visually/functionally on the lockscreen if paused
                    Button(intent: AddCheckpointIntent()) {
                        Label("Checkpoint", systemImage: "flag.fill")
                    }
                    .buttonStyle(.bordered)
                    .tint(.blue)
                    .opacity(context.state.lastActionName == "Paused" ? 0.5 : 1.0)
                    
                    Spacer()
                    
                    Button(intent: AddWaypointIntent()) {
                        Label("Waypoint", systemImage: "mappin.circle.fill")
                    }
                    .buttonStyle(.bordered)
                    .tint(.green)
                    .opacity(context.state.lastActionName == "Paused" ? 0.5 : 1.0)
                }
            }
            .padding()
            .activityBackgroundTint(Color.black.opacity(0.6))
            .activitySystemActionForegroundColor(Color.white)
            
        } dynamicIsland: { context in
            DynamicIsland {
                // 1. Leading Region
                DynamicIslandExpandedRegion(.leading) {
                    Label("\(String(format: "%.1f", context.state.distanceCovered/1000))km", systemImage: "figure.walk")
                }
                // 2. Trailing Region
                DynamicIslandExpandedRegion(.trailing) {
                    Text(context.attributes.startTime, style: .timer)
                        .multilineTextAlignment(.trailing)
                        .frame(width: 50)
                }
                // 3. Center Region (Added this to fix the 'Expanded' error)
                DynamicIslandExpandedRegion(.center) {
                    Text("Current Activity")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                // 4. Bottom Region
                DynamicIslandExpandedRegion(.bottom) {
                    HStack {
                        Button(intent: AddCheckpointIntent()) {
                            Label("Checkpoint", systemImage: "flag")
                        }.tint(.blue)
                        
                        Spacer()
                        
                        Button(intent: AddWaypointIntent()) {
                            Label("Waypoint", systemImage: "mappin")
                        }.tint(.green)
                    }
                }
            } compactLeading: {
                Image(systemName: "figure.walk").foregroundColor(.green)
            } compactTrailing: {
                if context.state.lastActionName == "Paused" {
                    Image(systemName: "pause.fill").foregroundColor(.yellow)
                } else {
                    Text(context.attributes.startTime, style: .timer)
                        .foregroundColor(.green)
                        .frame(width: 50)
                }
            } minimal: {
                if context.state.lastActionName == "Paused" {
                    Image(systemName: "pause.circle.fill").foregroundColor(.yellow)
                } else {
                    Image(systemName: "figure.walk")
                }
            }
        }
    }
}


#Preview("Notification", as: .content, using: TrackingAttributes(startTime: Date())) {
   LiveActivityLiveActivity()
} contentStates: {
    TrackingAttributes.ContentState(distanceCovered: 1200, lastActionName: "Started")
    TrackingAttributes.ContentState(distanceCovered: 2500, lastActionName: "Checkpoint Added")
}
