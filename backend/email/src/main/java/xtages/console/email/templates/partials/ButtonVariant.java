package xtages.console.email.templates.partials;

public enum ButtonVariant {
    PRIMARY("#008aff"),
    SUCCESS("#5cc9a7"),
    DANGER("#f25767"),
    WARNING("#ffbe3d"),
    DARK("#171347");

    private String color;

    ButtonVariant(String color) {
        this.color = color;
    }

    public String getColor() {
        return color;
    }
}
