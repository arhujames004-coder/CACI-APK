package com.example

import com.example.data.FeeConfig
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun percentage_fee_calculation_is_accurate() {
    val config = FeeConfig(type = "PERCENT", rate = 2.0, cap = 25.0)
    val (base, fee, total) = config.calculate(100.0)
    assertEquals(100.0, base, 0.001)
    assertEquals(2.0, fee, 0.001)
    assertEquals(102.0, total, 0.001)
  }

  @Test
  fun fee_cap_is_respected() {
    val config = FeeConfig(type = "PERCENT", rate = 5.0, cap = 10.0)
    val (base, fee, total) = config.calculate(1000.0)
    // 5% of 1000 is 50, but capped at 10
    assertEquals(1000.0, base, 0.001)
    assertEquals(10.0, fee, 0.001)
    assertEquals(1010.0, total, 0.001)
  }

  @Test
  fun flat_fee_calculation_is_accurate() {
    val config = FeeConfig(type = "FLAT", rate = 1.5, cap = 0.0)
    val (base, fee, total) = config.calculate(250.0)
    assertEquals(250.0, base, 0.001)
    assertEquals(1.5, fee, 0.001)
    assertEquals(251.5, total, 0.001)
  }

  @Test
  fun test_submit_tithe_slip_records_city_and_receipt() {
    val repo = com.example.data.StorehouseRepository()
    val initialTxCount = repo.transactions.value.size
    val tx = repo.submitTitheSlip(
      memberName = "Elder Kwabena Frimpong",
      city = "Kumasi",
      amount = 500.0,
      phone = "+233249998877",
      titheType = "Monthly Tithe (10%)",
      paymentMethod = "Mobile Money (Instant)",
      covenantNotes = "Covenant harvest"
    )

    assertEquals("Elder Kwabena Frimpong", tx.memberName)
    assertEquals("Kumasi", tx.city)
    assertEquals(500.0, tx.amount, 0.001)
    assertEquals("tithe", tx.fundCode)
    assertEquals("success", tx.status)
    assertEquals(initialTxCount + 1, repo.transactions.value.size)

    val member = repo.members.value.find { it.phone == "+233249998877" }
    org.junit.Assert.assertNotNull(member)
    assertEquals("Kumasi", member?.city)
  }
}


