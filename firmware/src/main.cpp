#include <Arduino.h>
#include "radar.hpp"

void setup() {
    Serial.begin(115200); // USB debug console only; no peripheral GPIO configured.
    Serial.println("RideGuard foundation: risk UNKNOWN; hardware drivers unavailable");
    Serial.println(rideguard::selectedRadar().sensor() == rideguard::Sensor::LD2451
        ? "Selected decoder: LD2451 (not implemented)" : "Selected decoder: LD2417 (not implemented)");
}

void loop() {
    // Yield to the RTOS without blocking a future cooperative sensor pipeline.
    yield();
}
