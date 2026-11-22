package com.mountainview.sms.lunch;

import jakarta.persistence.*;
import lombok.NoArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import com.mountainview.sms.student.LunchMethod;
import com.mountainview.sms.student.Student;
import java.time.LocalDate;
import java.math.BigDecimal;


@Entity 
@Table (name = "lunch_orders")
@Getter
@Setter
@NoArgsConstructor 

public class LunchOrder  {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(nullable = false)
    private LocalDate orderDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LunchStatus status;
    
    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal amountPaid = BigDecimal.ZERO ;
 
    private String itemDescription;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LunchMethod method;

 
    

    
}
