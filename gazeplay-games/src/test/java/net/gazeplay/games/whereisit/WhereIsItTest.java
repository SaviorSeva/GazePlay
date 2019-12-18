package net.gazeplay.games.whereisit;

import javafx.geometry.Dimension2D;
import net.gazeplay.IGameContext;
import net.gazeplay.commons.configuration.Configuration;
import net.gazeplay.commons.utils.stats.Stats;
import org.junit.Before;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.testfx.framework.junit5.ApplicationExtension;

import java.util.Random;

import static org.mockito.Mockito.when;

@ExtendWith(ApplicationExtension.class)
@RunWith(MockitoJUnitRunner.class)
class WhereIsItTest {
    // Test Mocks
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    IGameContext mockGameContext;

    @Mock
    Stats mockStats;

    @Mock
    Configuration mockConfig;

    @BeforeEach
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void shouldPickAndBuildRandomPictures() {
        WhereIsIt whereIsIt = new WhereIsIt(WhereIsItGameType.ANIMALNAME, 2, 2, false, mockGameContext, mockStats);
        when(mockConfig.getLanguage()).thenReturn("eng");

        Dimension2D mockDimension = new Dimension2D(20, 20);
        when(mockGameContext.getGamePanelDimensionProvider().getDimension2D()).thenReturn(mockDimension);

        Random random = new Random();
        RoundDetails randomPictures = whereIsIt.pickAndBuildRandomPictures(mockConfig, 4, random, 0);
        assert randomPictures.getPictureCardList().size() == 4;
    }

}
