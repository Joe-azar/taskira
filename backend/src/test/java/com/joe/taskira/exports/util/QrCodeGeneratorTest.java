package com.joe.taskira.exports.util;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.assertThat;

class QrCodeGeneratorTest {

    private final QrCodeGenerator generator = new QrCodeGenerator();

    @Test
    void generatedQrCodeDecodesBackToTheOriginalContent() throws Exception {
        String content = "https://taskira.test/projects/1/tickets/42";

        byte[] png = generator.generatePng(content);

        BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
        Result result = new MultiFormatReader().decode(
                new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(image)))
        );

        assertThat(result.getText()).isEqualTo(content);
    }

    @Test
    void generateDataUriProducesAValidBase64PngDataUri() {
        String dataUri = generator.generateDataUri("https://taskira.test");

        assertThat(dataUri).startsWith("data:image/png;base64,");
    }
}
