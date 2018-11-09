package com.mygdx.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class GameScreen extends ScreenAdapter {

    private enum STATE {
        PLAYING, GAME_OVER
    }

    private enum CAMERA_STATE {
        PERPENDICULAR, PERSPECTIVE
    }

    private SpriteBatch batch;
    private Texture snakeHead;
    private static final float MOVE_TIME = 0.1F;
    private float timer = MOVE_TIME;
    private static final float SNAKE_MOVEMENT = 32f;
    private float snakeX = 0, snakeY = 0;
    private static final int RIGHT = 0;
    private static final int LEFT = 1;
    private static final int UP = 2;
    private static final int DOWN = 3;
    private int snakeDirection = RIGHT;
    private Texture apple;
    private boolean appleAvailable = false;
    private float appleX, appleY;
    private Texture snakeBody;
    private Array<BodyPart> bodyParts = new Array<BodyPart>();
    private float snakeXBeforeUpdate = 0, snakeYBeforeUpdate = 0;
    private ShapeRenderer shapeRenderer;
    private static final int GRID_CELL = 32;
    private boolean directionSet;
    private boolean hasHit = false;
    private STATE state = STATE.PLAYING;
    private BitmapFont bitmapFont;
    private GlyphLayout layout = new GlyphLayout();
    private static final String GAME_OVER_TEXT = "Game Over... Eres un paquete!";
    private int score = 0;
    private static final int POINTS_PER_APPLE = 20;
    private static final float WORLD_WIDTH = 640f;
    private static final float WORLD_HEIGHT = 480f;
    private Viewport viewport;
    private Camera camera;
    private float cameraHeight = 480;
    private float fov = 55;
    private CAMERA_STATE camState;
    private float widthFactor = 1f;
    private float heightFactor = -1.75f;
    PerspectiveCamera pCam = new PerspectiveCamera(fov, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    OrthographicCamera oCam = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    OrthographicCamera fixedCam = new OrthographicCamera(Gdx.graphics.getWidth() * .75f, Gdx.graphics.getHeight() * .75f);
    private boolean fixedCamera = false;
    private float cooldown = 0;

    @Override
    public void show() {
        batch = new SpriteBatch();
        snakeHead = new Texture(Gdx.files.internal("snakehead.png"));
        apple = new Texture(Gdx.files.internal("apple.png"));
        snakeBody = new Texture(Gdx.files.internal("snakeBody.png"));
        shapeRenderer = new ShapeRenderer();
        bitmapFont = new BitmapFont();
        camera = oCam;
        pCam.near = 1f;
        pCam.far = 640 * 2f;
        oCam.position.set(WORLD_WIDTH / 2, WORLD_HEIGHT / 2, 0);
        pCam.position.set(WORLD_WIDTH / 2 * widthFactor, WORLD_HEIGHT / 2 * heightFactor, cameraHeight);
        oCam.lookAt(WORLD_WIDTH / 2, WORLD_HEIGHT / 2, 0);
        pCam.lookAt(WORLD_WIDTH / 2, WORLD_HEIGHT / 2, 3);
        camera.update();
        viewport = new FitViewport(WORLD_WIDTH, WORLD_HEIGHT, camera);
        camState = CAMERA_STATE.PERSPECTIVE;
    }

    private void checkCooldown(float delta) {
        if (cooldown > 0f) {
            cooldown -= delta;
        } else if (cooldown < 0f) {
            cooldown = 0f;
        }
    }

    @Override
    public void render(float delta) {
        checkCooldown(delta);
        switch (state) {
            case PLAYING:
                queryInput();
                updateSnake(delta);
                checkAndPlaceApple();
                break;
            case GAME_OVER:
                checkForRestart();
                break;
        }

        clearScreen();
        drawGrid();
        draw();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height);
    }

    private void addToScore() {
        score += POINTS_PER_APPLE;
    }

    private void checkForRestart() {
        if (Gdx.input.isKeyPressed(Input.Keys.SPACE)) {
            doRestart();
        }
    }

    private void doRestart() {
        state = STATE.PLAYING;
        bodyParts.clear();
        snakeDirection = RIGHT;
        directionSet = false;
        timer = MOVE_TIME;
        snakeX = 0;
        snakeY = 0;
        snakeXBeforeUpdate = 0;
        snakeYBeforeUpdate = 0;
        appleAvailable = false;
        score = 0;
    }

    private void updateSnake(float delta) {
        if (!hasHit) {
            timer -= delta;
            if (timer <= 0) {
                timer = MOVE_TIME;
                moveSnake();
                checkForOutOfBounds();
                updateBodyPartsPosition();
                checkSnakeBodyCollision();
                directionSet = false;
            }
        }
    }

    private void checkSnakeBodyCollision() {
        for (BodyPart bodyPart : bodyParts) {
            if (bodyPart.x == snakeX && bodyPart.y == snakeY) {
                state = STATE.GAME_OVER;
            }
        }
    }

    private void updateIfNotOppositeDirection(int newSnakeDirection, int oppositeDirection) {
        if (snakeDirection != oppositeDirection || bodyParts.size == 0) {
            snakeDirection = newSnakeDirection;
        }
    }

    private void drawGrid() {
        shapeRenderer.setProjectionMatrix(camera.projection);
        shapeRenderer.setTransformMatrix(camera.view);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        for (int x = 0; x < viewport.getWorldWidth(); x += GRID_CELL) {
            for (int y = 0; y < viewport.getWorldHeight(); y += GRID_CELL) {
                shapeRenderer.rect(x, y, GRID_CELL, GRID_CELL);

            }
        }
        shapeRenderer.end();
    }

    private void updateDirection(int newSnakeDirection) {
        if (!directionSet && snakeDirection != newSnakeDirection) {
            directionSet = true;
            switch (newSnakeDirection) {
                case LEFT:
                    updateIfNotOppositeDirection(newSnakeDirection, RIGHT);
                    break;
                case RIGHT:
                    updateIfNotOppositeDirection(newSnakeDirection, LEFT);
                    break;
                case UP:
                    updateIfNotOppositeDirection(newSnakeDirection, DOWN);
                    break;
                case DOWN:
                    updateIfNotOppositeDirection(newSnakeDirection, UP);
                    break;
            }
        }
    }

    private void clearScreen() {
        Gdx.gl.glClearColor(Color.BLACK.r, Color.BLACK.g, Color.BLACK.b, Color.BLACK.a);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
    }

    private void draw() {
        if (fixedCamera) {
            fixedCam.position.set(snakeX, snakeY, 0);
            fixedCam.lookAt(snakeX, snakeY, 0);
        }
        camera.update();
        batch.setProjectionMatrix(camera.projection);
        batch.setTransformMatrix(camera.view);
        batch.begin();
        batch.draw(snakeHead, snakeX, snakeY);
        for (BodyPart bodyPart : bodyParts) {
            bodyPart.draw(batch);
        }

        if (appleAvailable) {
            batch.draw(apple, appleX, appleY);
        }

        if (state == STATE.GAME_OVER) {
            layout.setText(bitmapFont, GAME_OVER_TEXT);
            bitmapFont.draw(batch, layout, (viewport.getWorldWidth() - layout.width) / 2, (viewport.getWorldHeight() - layout.height) / 2);

        }
        drawScore();
        batch.end();
    }

    private void checkForOutOfBounds() {
        if (snakeX >= viewport.getWorldWidth()) {
            snakeX = 0;
        }
        if (snakeX < 0) {
            snakeX = viewport.getWorldWidth() - SNAKE_MOVEMENT;
        }

        if (snakeY >= viewport.getWorldHeight()) {
            snakeY = 0;
        }
        if (snakeY < 0) {
            snakeY = viewport.getWorldHeight() - SNAKE_MOVEMENT;
        }
    }

    private void moveSnake() {
        snakeXBeforeUpdate = snakeX;
        snakeYBeforeUpdate = snakeY;

        switch (snakeDirection) {
            case RIGHT: {
                snakeX += SNAKE_MOVEMENT;
                return;
            }
            case LEFT: {
                snakeX -= SNAKE_MOVEMENT;
                return;
            }
            case UP: {
                snakeY += SNAKE_MOVEMENT;
                return;
            }
            case DOWN: {
                snakeY -= SNAKE_MOVEMENT;
                return;
            }
        }
    }

    private void drawScore() {
        if (state == STATE.PLAYING) {
            String scoreAsString = Integer.toString(score);
            layout.setText(bitmapFont, scoreAsString);
            bitmapFont.draw(batch, layout, (viewport.getWorldWidth() - layout.width) / 2, (4 * viewport.getWorldHeight() / 5) - layout.height / 2);
        }
    }

    private void queryInput() {
        boolean lPressed = Gdx.input.isKeyPressed(Input.Keys.LEFT);
        boolean rPressed = Gdx.input.isKeyPressed(Input.Keys.RIGHT);
        boolean uPressed = Gdx.input.isKeyPressed(Input.Keys.UP);
        boolean dPressed = Gdx.input.isKeyPressed(Input.Keys.DOWN);
        boolean wPressed = Gdx.input.isKeyPressed(Input.Keys.W);
        boolean sPressed = Gdx.input.isKeyPressed(Input.Keys.S);
        boolean tabPressed = Gdx.input.isKeyPressed(Input.Keys.TAB);
        boolean qPressed = Gdx.input.isKeyPressed(Input.Keys.Q);
        boolean ePressed = Gdx.input.isKeyPressed(Input.Keys.E);
        boolean aPressed = Gdx.input.isKeyPressed(Input.Keys.A);
        boolean dKeyPressed = Gdx.input.isKeyPressed(Input.Keys.D);
        boolean fPressed = Gdx.input.isKeyPressed(Input.Keys.F);

        if (lPressed) {
            updateDirection(LEFT);
        }
        if (rPressed) {
            updateDirection(RIGHT);
        }
        if (uPressed) {
            updateDirection(UP);
        }
        if (dPressed) {
            updateDirection(DOWN);
        }
        if (cooldown == 0f) {

            if (fPressed) {
                if (fixedCamera) {
                    camera = oCam;
                    camera.update();
                    fixedCamera = false;
                } else {

                    camera = fixedCam;

                    camera.update();
                    fixedCamera = true;
                }
                cooldown = .5f;
            }
            if (!fixedCamera) {

                if (tabPressed) {
                    switch (camState) {
                        case PERPENDICULAR:
                            camera = pCam;
                            //pCam.position.set(WORLD_WIDTH / 2, WORLD_HEIGHT / -1.5f, cameraHeight);
                            camState = CAMERA_STATE.PERSPECTIVE;
                            break;
                        case PERSPECTIVE:
                            camera = oCam;
                            //camera.position.set(WORLD_WIDTH / 2, WORLD_HEIGHT / 2, 0);
                            camState = CAMERA_STATE.PERPENDICULAR;

                            break;
                    }
                    camera.update();
                    cooldown = .5f;
                }
            }

        }
        if (camState == CAMERA_STATE.PERSPECTIVE) {
            if (wPressed) {
                pCam.position.set(WORLD_WIDTH / 2 * widthFactor, WORLD_HEIGHT / 2 * heightFactor, cameraHeight += 5);
            }
            if (sPressed) {
                pCam.position.set(WORLD_WIDTH / 2 * widthFactor, WORLD_HEIGHT / 2 * heightFactor, cameraHeight -= 5);
            }
            if (qPressed) {
                fov += .25f;
                pCam.fieldOfView = fov;
            }
            if (ePressed) {
                fov -= .25f;
                pCam.fieldOfView = fov;
            }
            if (aPressed) {
                widthFactor -= .25f;
                pCam.position.set(WORLD_WIDTH / 2 * widthFactor, WORLD_HEIGHT / 2 * heightFactor, cameraHeight);
            }
            if (dKeyPressed) {
                widthFactor += .25f;
                pCam.position.set(WORLD_WIDTH / 2 * widthFactor, WORLD_HEIGHT / 2 * heightFactor, cameraHeight);
            }
            camera.update();
        }
    }

    private void checkAndPlaceApple() {
        checkAppleCollision();
        if (!appleAvailable) {
            do {
                appleX = MathUtils.random((int) (viewport.getWorldWidth()
                        / SNAKE_MOVEMENT) - 1) * SNAKE_MOVEMENT;
                appleY = MathUtils.random((int) (viewport.getWorldHeight()
                        / SNAKE_MOVEMENT) - 1) * SNAKE_MOVEMENT;
                appleAvailable = true;
            } while (appleX == snakeX && appleY == snakeY);
        }
    }

    private void checkAppleCollision() {
        if (appleAvailable && appleX == snakeX && appleY == snakeY) {
            BodyPart bodypart = new BodyPart(snakeBody);
            bodypart.updateBodyPosition(snakeX, snakeY);
            bodyParts.insert(0, bodypart);
            addToScore();
            appleAvailable = false;
        }
    }

    private void updateBodyPartsPosition() {
        if (bodyParts.size > 0) {
            BodyPart bodyPart = bodyParts.removeIndex(0);
            bodyPart.updateBodyPosition(snakeXBeforeUpdate,
                    snakeYBeforeUpdate);
            bodyParts.add(bodyPart);
        }
    }

    private class BodyPart {

        private float x, y;
        private Texture texture;

        public BodyPart(Texture texture) {
            this.texture = texture;
        }

        public void updateBodyPosition(float x, float y) {
            this.x = x;
            this.y = y;
        }

        public void draw(Batch batch) {
            if (!(x == snakeX && y == snakeY)) {
                batch.draw(texture, x, y);
            }
        }
    }

}
