package com.amigoscode.testing.payment;

import com.amigoscode.testing.customer.Customer;
import com.amigoscode.testing.customer.CustomerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentService {

    private static final List<Currency> ACCEPTED_CURRENCIES = List.of(Currency.USD, Currency.GBP);

    private final CustomerRepository customerRepository;
    private final PaymentRepository paymentRepository;
    private final CardPaymentCharger cardPaymentCharger;

    @Autowired
    public PaymentService(CustomerRepository customerRepository, PaymentRepository paymentRepository, CardPaymentCharger cardPaymentCharger) {
        this.customerRepository = customerRepository;
        this.paymentRepository = paymentRepository;
        this.cardPaymentCharger = cardPaymentCharger;
    }

    void chargeCard(UUID customerId, PaymentRequest paymentRequest) {

        // 1. Does customer exist if not throw
        boolean isCustomerFound = customerRepository.findById(customerId).isPresent();
        if(!isCustomerFound) {
            throw new IllegalStateException(String.format("Customer with id [%s] not found", customerId));
        }

        // 2. Do we support the currency if not throw
        Payment payment = paymentRequest.getPayment();
        boolean isCurrencySupported = ACCEPTED_CURRENCIES.stream().anyMatch(c -> c.equals(payment.getCurrency()));
        if(!isCurrencySupported) {
            throw new IllegalStateException(String.format("Currency [%s] not supported", payment.getCurrency()));
        }

        // 3. Charge card
        CardPaymentCharge cardPaymentCharge = cardPaymentCharger.chargeCard(
                payment.getSource(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getDescription()
        );

        // 4. If not debited throw
        if(!cardPaymentCharge.isCardDebited()) {
            throw new IllegalStateException(String.format("Card not debited for customer %s", customerId));
        }

        // 5. Insert payment
        payment.setCustomerId(customerId);

        paymentRepository.save(payment);

        // 6. TODO: send sms
    }
}
