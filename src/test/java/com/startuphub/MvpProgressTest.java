package com.startuphub;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.startuphub.model.Mvp;
import com.startuphub.model.MvpFeature;
import com.startuphub.model.MvpStatus;
import org.junit.jupiter.api.Test;

class MvpProgressTest {

    private Mvp mvpWith(int total, int completed) {
        Mvp mvp = new Mvp(null, "StartupHub MVP", null, "1.0", MvpStatus.IN_DEVELOPMENT, null, null);
        for (int i = 0; i < total; i++) {
            MvpFeature f = new MvpFeature(mvp, "Feature " + i, null, null);
            if (i < completed) f.markCompleted(true);
            mvp.getFeatures().add(f);
        }
        return mvp;
    }

    @Test
    void noFeaturesMeansZeroPercentNotADivisionError() {
        assertEquals(0, mvpWith(0, 0).progressPercent());
    }

    @Test
    void sixOfTenIsSixtyPercent() {
        Mvp mvp = mvpWith(10, 6);
        assertEquals(60, mvp.progressPercent());
        assertEquals(6, mvp.completedFeatures());
        assertEquals(10, mvp.totalFeatures());
    }

    @Test
    void allDoneIsOneHundredAndReopeningLowersIt() {
        Mvp mvp = mvpWith(3, 3);
        assertEquals(100, mvp.progressPercent());
        mvp.getFeatures().get(0).markCompleted(false);
        assertEquals(67, mvp.progressPercent());
    }
}
