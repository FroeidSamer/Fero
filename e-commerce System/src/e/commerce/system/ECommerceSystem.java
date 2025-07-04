package e.commerce.system;

/**
 *
 * @author Froeid
 */
import java.time.LocalDate;
import java.util.*;

interface Shippable {

    String getName();

    double getWeight();
}

abstract class Product {

    String name;
    int price;
    int quantity;

    public Product(String name, int price, int quantity) {
        this.name = name;
        this.price = price;
        this.quantity = quantity;
    }

    public String getName() {
        return name;
    }

    public int getPrice() {
        return price;
    }

    public int getQuantity() {
        return quantity;
    }

    public void reduceQuantity(int amount) {
        this.quantity -= amount;
    }
}

class PerishableProduct extends Product {

    LocalDate expiryDate;

    public PerishableProduct(String name, int price, int quantity, LocalDate expiryDate) {
        super(name, price, quantity);
        this.expiryDate = expiryDate;
    }

    public boolean isExpired() {
        return expiryDate.isBefore(LocalDate.now());
    }
}

class ShippableProduct extends Product implements Shippable {

    double weight;

    public ShippableProduct(String name, int price, int quantity, double weight) {
        super(name, price, quantity);
        this.weight = weight;
    }

    public double getWeight() {
        return weight;
    }
}

class PerishableShippableProduct extends PerishableProduct implements Shippable {

    double weight;

    public PerishableShippableProduct(String name, int price, int quantity, LocalDate expiryDate, double weight) {
        super(name, price, quantity, expiryDate);
        this.weight = weight;
    }

    public double getWeight() {
        return weight;
    }
}

class Customer {

    String name;
    int balance;

    public Customer(String name, int balance) {
        this.name = name;
        this.balance = balance;
    }

    public boolean canAfford(int amount) {
        return balance >= amount;
    }

    public void deduct(int amount) {
        balance -= amount;
    }

    public int getBalance() {
        return balance;
    }
}

class CartItem {

    Product product;
    int quantity;

    public CartItem(Product product, int quantity) {
        this.product = product;
        this.quantity = quantity;
    }

    public int getTotalPrice() {
        return product.getPrice() * quantity;
    }
}

class Cart {

    List<CartItem> items = new ArrayList<>();

    public void add(Product product, int quantity) {
        if (quantity > product.getQuantity()) {
            throw new IllegalArgumentException("Not enough stock for product: " + product.getName());
        }
        items.add(new CartItem(product, quantity));
    }

    public List<CartItem> getItems() {
        return items;
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public int getSubtotal() {
        return items.stream().mapToInt(CartItem::getTotalPrice).sum();
    }

    public List<Shippable> getShippableItems() {
        List<Shippable> shippables = new ArrayList<>();
        for (CartItem item : items) {
            if (item.product instanceof Shippable) {
                for (int i = 0; i < item.quantity; i++) {
                    shippables.add((Shippable) item.product);
                }
            }
        }
        return shippables;
    }
}

class ShippingService {

    public static void ship(List<Shippable> items) {
        System.out.println("** Shipment notice **");
        double totalWeight = 0;
        Map<String, Integer> counts = new HashMap<>();
        Map<String, Double> weights = new HashMap<>();

        for (Shippable item : items) {
            counts.put(item.getName(), counts.getOrDefault(item.getName(), 0) + 1);
            weights.put(item.getName(), item.getWeight());
            totalWeight += item.getWeight();
        }

        for (String name : counts.keySet()) {
            System.out.println(counts.get(name) + "x " + name);
            System.out.println((int) (weights.get(name) * 1000) + "g");
        }

        System.out.println("Total package weight " + totalWeight + "kg");
    }
}

class CheckoutService {

    public static void checkout(Customer customer, Cart cart) {
        if (cart.isEmpty()) {
            throw new IllegalStateException("Cart is empty.");
        }

        for (CartItem item : cart.getItems()) {
            if (item.product instanceof PerishableProduct) {
                if (((PerishableProduct) item.product).isExpired()) {
                    throw new IllegalStateException(item.product.getName() + " is expired.");
                }
            }
            if (item.quantity > item.product.getQuantity()) {
                throw new IllegalStateException("Not enough stock for product: " + item.product.getName());
            }
        }

        int subtotal = cart.getSubtotal();
        int shippingFee = cart.getShippableItems().isEmpty() ? 0 : 30;
        int total = subtotal + shippingFee;

        if (!customer.canAfford(total)) {
            throw new IllegalStateException("Insufficient balance.");
        }

        if (!cart.getShippableItems().isEmpty()) {
            ShippingService.ship(cart.getShippableItems());
        }

        customer.deduct(total);

        for (CartItem item : cart.getItems()) {
            item.product.reduceQuantity(item.quantity);
        }

        System.out.println("** Checkout receipt **");
        for (CartItem item : cart.getItems()) {
            System.out.println(item.quantity + "x " + item.product.getName());
            System.out.println(item.product.getPrice() * item.quantity);
        }
        System.out.println("----------------------");
        System.out.println("Subtotal      " + subtotal);
        System.out.println("Shipping      " + shippingFee);
        System.out.println("Amount        " + total);
        System.out.println("Balance       " + customer.getBalance());
    }
}

public class ECommerceSystem {

    public static void main(String[] args) {
        Product cheese = new PerishableShippableProduct("Cheese", 100, 10, LocalDate.now().plusDays(1), 0.2);
        Product biscuits = new PerishableProduct("Biscuits", 150, 5, LocalDate.now().plusDays(3));
        Product tv = new ShippableProduct("TV", 400, 3, 0.7);
        Product scratchCard = new Product("Scratch Card", 50, 20) {
        };

        Customer customer = new Customer("Ali", 1000);
        Cart cart = new Cart();

        cart.add(cheese, 2);
        cart.add(biscuits, 1);
        cart.add(scratchCard, 1);

        CheckoutService.checkout(customer, cart);
    }

}
