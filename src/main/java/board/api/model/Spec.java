package board.api.model;

import lombok.Data;

@Data
public class Spec {
    private int contactLength;
    private double sideCut;
    private double noseWidth;
    private double tailWidth;
    private double waistWidth;
    private Stance stance;
    private int flex;
    private int weightRange;
}
