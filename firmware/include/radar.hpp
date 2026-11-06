#pragma once
#include <cstddef>
#include <cstdint>
#include <optional>

#if defined(RIDEGUARD_RADAR_LD2451) == defined(RIDEGUARD_RADAR_LD2417)
#error "Select exactly one radar driver"
#endif

namespace rideguard {
enum class Sensor { LD2451, LD2417 };
enum class Availability { Unavailable, Valid, Stale, Fault };
enum class DistanceDefinition { Unspecified, Radial, AxisY };
struct RadarSample {
    Sensor sensor;
    Availability availability = Availability::Unavailable;
    DistanceDefinition distance_definition = DistanceDefinition::Unspecified;
    std::optional<float> distance_m;
    std::optional<float> speed_mps;
    std::optional<uint32_t> target_id; // Sensor-local hint; stability unverified.
    uint32_t received_monotonic_ms = 0;
};
enum class DecodeStatus { NotImplemented, Incomplete, Invalid, Decoded };
class RadarDecoder {
public:
    virtual ~RadarDecoder() = default;
    virtual Sensor sensor() const = 0;
    virtual DecodeStatus decode(const uint8_t* bytes, size_t size, RadarSample& output) = 0;
};
RadarDecoder& selectedRadar();
}
