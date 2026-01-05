//
//  Item.swift
//  sportmapapp
//
//  Created by Anne-Riin Peep on 05.01.2026.
//

import Foundation
import SwiftData

@Model
final class Item {
    var timestamp: Date
    
    init(timestamp: Date) {
        self.timestamp = timestamp
    }
}
