//
//  TrackingIntents.swift
//  sport
//
//  Created by Anne-Riin Peep on 06.01.2026.
//

import AppIntents
import Foundation

// Intent for adding a Checkpoint
struct AddCheckpointIntent: LiveActivityIntent {
    static var title: LocalizedStringResource = "Add Checkpoint"
    
    // This function runs when the button is clicked
    func perform() async throws -> some IntentResult {
        // 1. Get shared defaults
        let shared = UserDefaults(suiteName: "group.taltech.anpeep.sport")
        
        // 2. Logic to increment checkpoints
        let currentCount = shared?.integer(forKey: "checkpoint_count") ?? 0
        shared?.set(currentCount + 1, forKey: "checkpoint_count")
        
        // 3. Mark that a change happened
        shared?.set("CP", forKey: "pending_action")

        print("Checkpoint added via Live Activity!")
        return .result()
    }
}

// Intent for adding a Waypoint
struct AddWaypointIntent: LiveActivityIntent {
    static var title: LocalizedStringResource = "Add Waypoint"
    
    func perform() async throws -> some IntentResult {
        let shared = UserDefaults(suiteName: "group.taltech.anpeep.sport")
        
        let currentCount = shared?.integer(forKey: "waypoint_count") ?? 0
        shared?.set(currentCount + 1, forKey: "waypoint_count")
        
        shared?.set("WP", forKey: "pending_action")

        print("Waypoint added via Live Activity!")
        return .result()
    }
}
