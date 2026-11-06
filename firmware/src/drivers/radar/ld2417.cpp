#include "radar.hpp"
namespace rideguard {
class LD2417Decoder final : public RadarDecoder {
public:
    Sensor sensor() const override { return Sensor::LD2417; }
    DecodeStatus decode(const uint8_t*, size_t, RadarSample& output) override {
        output = RadarSample{sensor()};
        return DecodeStatus::NotImplemented;
    }
};
RadarDecoder& selectedRadar() { static LD2417Decoder decoder; return decoder; }
}
