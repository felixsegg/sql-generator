package controller;

import access.JpaAccess;
import access.JpaAccessHolder;
import entitytype.EntityTypeSelectionFX;
import entitytype.generator.UMLGenerator;
import entitytype.generator.impl.UMLGeneratorImpl;
import util.FXUtil;
import jakarta.persistence.metamodel.EntityType;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.*;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import service.ServiceSql;
import service.ServiceSqlImpl;
import util.SimpleStringFuture;

import java.io.InputStream;
import java.net.URL;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.function.Consumer;

public class GeneratorController implements Initializable {
    @FXML
    private Label promptHelpLabel, entityTypeHelpLabel;
    @FXML
    private TextArea promptTA, sqlTA;
    @FXML
    private CheckBox selectAllCheckBox;
    @FXML
    private VBox root, selectionCheckBoxVBox;
    
    private final ServiceSql service = ServiceSqlImpl.getInstance();
    private final JpaAccess access = JpaAccessHolder.get();
    private final UMLGenerator umlGenerator = UMLGeneratorImpl.getInstance();
    private EntityTypeSelectionFX selection;
    
    private final SimpleStringFuture result = new SimpleStringFuture();
    
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        Set<EntityType<?>> entityTypes = access.getMetamodel().getEntities();
        selection = new EntityTypeSelectionFX(entityTypes);
        
        sqlTA.setTooltip(new Tooltip("Query must start with SELECT and may not contain a semicolon"));
        
        initializeEntityTypeSelection();
        initializeHelpLabels();
    }
    
    private void initializeEntityTypeSelection() {
        Set<EntityType<?>> entityTypes = selection.getEntityTypes();
        List<EntityType<?>> entityTypesSorted = entityTypes.stream().sorted(Comparator.comparing(EntityType::getName)).toList();
        
        for (EntityType<?> et : entityTypesSorted) {
            CheckBox cb = new CheckBox(et.getName());
            cb.setSelected(true);
            cb.selectedProperty().bindBidirectional(selection.getBooleanPropertyForEntityType(et));
            cb.setOnAction(e -> {
                if ((!cb.isSelected() && selectAllCheckBox.isSelected()) || cb.isSelected()) // Easy way to prevent unnecessary checks for all fields
                    selectAllCheckBox.setSelected(selection.areAllSelected());
            });
            selectionCheckBoxVBox.getChildren().add(cb);
        }
        
        selectAllCheckBox.setSelected(true);
        selectAllCheckBox.setOnAction(e -> {
            if (selectAllCheckBox.isSelected()) selection.selectAll();
            else selection.unselectAll();
        });
    }
    
    private void initializeHelpLabels() {
        initializeHelpLabel(promptHelpLabel, this::showPromptHelp);
        initializeHelpLabel(entityTypeHelpLabel, this::showEntityTypeHelp);
    }
    
    private void initializeHelpLabel(Label helpLabel, Runnable showWindow) {
        helpLabel.setTooltip(new Tooltip("Hilfe anzeigen"));
        helpLabel.setOnMouseReleased(e -> {
            if (helpLabel.contains(e.getX(), e.getY())) {
                showWindow.run();
            }
        });
    }
    
    private void showEntityTypeHelp() {
        VBox contentPane = new VBox(10);
        contentPane.setPadding(new Insets(10));
        
        Label headerLabel = new Label("Hilfe für Entitätstypenauswahl");
        headerLabel.setStyle("-fx-font-size: 20px;");
        
        
        TextArea helpArea = new TextArea("""
                In the overview, select the entity types of the domain model that are to be transferred to the context of the prompt.
                Please note that all necessary “connectors” of the respective entity types must also be selected.
                For orientation purposes, you will find a graphic below that visualizes the relationships."""
        );
        helpArea.setWrapText(true);
        helpArea.setEditable(false);
        helpArea.setFocusTraversable(false);
        
        helpArea.setPrefHeight(Region.USE_COMPUTED_SIZE);
        
        ProgressIndicator spinner = new ProgressIndicator();
        
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setDisable(true);
        scrollPane.setPrefViewportWidth(600);
        scrollPane.setPrefViewportHeight(400);
        
        Consumer<Image> setImageConsumer = FXUtil.initializeZoomableScrollPane(scrollPane);
        
        StackPane stack = new StackPane(scrollPane, spinner);
        StackPane.setAlignment(spinner, Pos.CENTER);
        VBox.setVgrow(stack, Priority.ALWAYS);
        
        Separator separator = new Separator(Orientation.HORIZONTAL);
        
        contentPane.getChildren().addAll(headerLabel, separator, helpArea, stack);
        
        Task<InputStream> loadGraphTask = new Task<>() {
            @Override
            protected InputStream call() {
                return umlGenerator.getGraphImageStream(selection.getEntityTypes());
            }
        };
        loadGraphTask.setOnSucceeded(e -> {
            Image img = new Image(loadGraphTask.getValue());
            setImageConsumer.accept(img);
            spinner.setVisible(false);
            scrollPane.setDisable(false);
        });
        loadGraphTask.setOnFailed(e -> {
            spinner.setVisible(false);
            stack.getChildren().add(new Label("Fehler beim Laden des Diagramms"));
        });
        
        new Thread(loadGraphTask, "GraphLoader").start();
        
        Stage popup = FXUtil.createModalPopup(root.getScene().getWindow(), contentPane, root.getWidth() / 8, root.getHeight() / 8, "Help - Entity type selection");
        contentPane.prefHeightProperty().bind(popup.heightProperty());
        popup.setWidth(800);
        popup.setHeight(700);
        popup.show();
    }
    
    private void showPromptHelp() {
        VBox contentPane = new VBox(10);
        contentPane.setPadding(new Insets(10));
        
        Label headerLabel = new Label("Hilfe für Promptentwurf");
        headerLabel.setStyle("-fx-font-size: 20px;");
        Separator separator = new Separator(Orientation.HORIZONTAL);
        
        TextArea helpArea = new TextArea("""
                Select a suitable language model and formulate your query as clearly and precisely as possible to achieve optimal results.
                
                Recommended models:
                - Claude: Very reliable results, especially for complex queries.
                - DeepSeek: Consistent quality, particularly robust for highly complex queries.
                - Gemini: Stable performance for medium and simple queries.
                - ChatGPT: More suitable for simple queries, less reliable for complex ones.
                
                Tips for formulating your prompt:
                - Use precise, technical descriptions, especially for complex queries.
                - Avoid ambiguities; specify concrete entity and attribute names.
                - Formulate conditions explicitly (e.g., date ranges, attribute values).
                - For medium complexity, ensure clear structuring and completeness.
                
                The more precise and technical your input, the better and more consistent the generated SQL queries will be."""
        );
        helpArea.setWrapText(true);
        helpArea.setEditable(false);
        helpArea.setFocusTraversable(false);
        VBox.setVgrow(helpArea, Priority.ALWAYS);
        
        contentPane.getChildren().addAll(headerLabel, separator, helpArea);
        
        Stage popup = FXUtil.createModalPopup(root.getScene().getWindow(), contentPane, root.getWidth() / 8, root.getHeight() / 8, "Help - Prompt");
        contentPane.prefHeightProperty().bind(popup.heightProperty());
        popup.setWidth(400.0);
        popup.setHeight(300.0);
        popup.show();
    }
    
    public Future<String> getResult() {
        return result;
    }
    
    @FXML
    private void copyPrompt() {
        if (!promptTA.getText().isBlank()) {
            ClipboardContent content = new ClipboardContent();
            content.putString(selection.getFullPrompt(promptTA.getText()));
            Clipboard.getSystemClipboard().setContent(content);
        } else {
            FXUtil.signalBorder(promptTA);
        }
    }
    
    @FXML
    private void testQuery() {
        VBox contentPane = new VBox(10);
        contentPane.setPadding(new Insets(10.0));
        
        Label headerLabel = new Label("Testausführung");
        headerLabel.setStyle("-fx-font-size: 20px;");
        
        ProgressIndicator spinner = new ProgressIndicator();
        
        TextArea ta = new TextArea();
        ta.setEditable(false);
        ta.setStyle("-fx-border-width: 2px;");
        ta.setWrapText(false);
        ta.setDisable(false);
        ta.setMaxHeight(Double.MAX_VALUE);
        
        StackPane stack = new StackPane(ta, spinner);
        StackPane.setAlignment(spinner, Pos.CENTER);
        VBox.setVgrow(stack, Priority.ALWAYS);
        
        Separator separator = new Separator();
        
        contentPane.getChildren().addAll(headerLabel, separator, stack);
        
        Stage popup = FXUtil.createModalPopup(root.getScene().getWindow(), contentPane, root.getWidth() / 3, root.getHeight() / 3, "Testausführung");
        contentPane.prefHeightProperty().bind(popup.heightProperty());
        popup.setWidth(500.0);
        popup.setHeight(400.0);
        popup.show();
        
        Task<String> task = new Task<>() {
            @Override
            protected String call() {
                return service.getErrorsFor(sqlTA.getText());
            }
        };
        
        task.setOnSucceeded(evt -> {
            spinner.setVisible(false);
            ta.setDisable(false);
            String messages = task.getValue();
            if (messages != null) {
                ta.setText(messages);
                ta.setStyle("-fx-border-color: red;");
                ta.setOnMouseClicked(e -> {
                    ClipboardContent content = new ClipboardContent();
                    content.putString(messages);
                    Clipboard.getSystemClipboard().setContent(content);
                    ta.selectAll();
                });
                ta.setTooltip(new Tooltip("Klicken um in die Zwischenablage zu kopieren."));
            } else {
                ta.setText("Erfolgreich ausgeführt!");
                ta.setStyle("-fx-border-color: green;");
            }
        });
        
        task.setOnFailed(evt -> {
            spinner.setVisible(false);
            ta.setDisable(false);
            ta.setText("Fehler im Programm: " + task.getException().getMessage());
        });
        
        new Thread(task).start();
    }
    
    @FXML
    private void confirm() {
        String sql = sqlTA.getText();
        if (service.isQuery(sql))
            FXUtil.signalBorder(sqlTA);
        else result.complete(sql);
    }
    
    @FXML
    private void cancel() {
        result.cancel(true);
        Stage stage = (Stage) root.getScene().getWindow();
        if (stage != null)
            stage.close();
    }
}
