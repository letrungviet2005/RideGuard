#include "radar.hpp"
namespace rideguard {
class LD2451Decoder final : public RadarDecoder {
public:
    Sensor sensor() const override { return Sensor::LD2451; }
    DecodeStatus decode(const uint8_t*, size_t, RadarSample& output) override {
        output = RadarSample{sensor()};
        return DecodeStatus::NotImplemented;
    }
};
RadarDecoder& selectedRadar() { static LD2451Decoder decoder; return decoder; }
}
