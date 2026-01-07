//
//  TrackingAttributes.swift
//  sport
//
//  Created by Anne-Riin Peep on 06.01.2026.
//


import Foundation
import ActivityKit

struct TrackingAttributes: ActivityAttributes {
    public struct ContentState: Codable, Hashable {
        // Dynamic data that updates on the lock screen
        var distanceCovered: Double
        var lastActionName: String // e.g., "Checkpoint added"
    }

    // Static data (doesn't change)
    var startTime: Date
}