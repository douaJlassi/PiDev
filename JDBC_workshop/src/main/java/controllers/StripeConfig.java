package controllers;

public class StripeConfig {
    // Get your keys from: https://dashboard.stripe.com/apikeys
    // Never hardcode real keys — use environment variables in production
    public static final String SECRET_KEY      = System.getenv("STRIPE_SECRET_KEY") != null
            ? System.getenv("STRIPE_SECRET_KEY")
            : "sk_test_YOUR_NEW_KEY_HERE";
    public static final String PUBLISHABLE_KEY = System.getenv("STRIPE_PUBLISHABLE_KEY") != null
            ? System.getenv("STRIPE_PUBLISHABLE_KEY")
            : "pk_test_YOUR_NEW_KEY_HERE";
}