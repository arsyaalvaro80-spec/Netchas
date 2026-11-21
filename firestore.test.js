const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_owner_123";
const BOB_UID = "bob_other_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function nowPast(offsetMs = 2000) {
  return new Date(Date.now() - offsetMs);
}

function validSettingsPayload(uid, ts = nowPast()) {
  return {
    userId: uid,
    businessName: "NetKas Wi-Fi Nusantara",
    businessPhone: "081234567890",
    businessAddress: "Jl. Merdeka No. 12, Bandung",
    bankAccountInfo: "BCA 1234567890 a.n. NetKas",
    receiptFooterText: "Terima kasih atas pembayaran tepat waktu Anda.\nSimpan kuitansi ini sebagai bukti sah.",
    reminderDaysBefore: 3,
    remindersEnabled: true,
    createdAt: ts,
    updatedAt: ts,
  };
}

function validCustomerPayload(uid, ts = nowPast()) {
  return {
    userId: uid,
    name: "Budi Santoso",
    phone: "081298765432",
    email: "budi@example.com",
    address: "Blok A No. 5 RT 02",
    packageName: "20 Mbps Home",
    monthlyFee: 150000,
    dueDay: 10,
    active: true,
    notes: "Router TP-Link lantai 1",
    createdAt: ts,
    updatedAt: ts,
  };
}

function validPaymentPayload(uid, customerId, yearMonth = "2026-11", ts = nowPast()) {
  return {
    userId: uid,
    customerId,
    customerName: "Budi Santoso",
    packageName: "20 Mbps Home",
    yearMonth,
    amount: 150000,
    paymentMethod: "BANK_TRANSFER",
    bankReference: "BCA #TRX9921",
    receiptNumber: "NK-202611-001",
    paidDateMillis: Date.now() - 5000,
    notes: "Lunas tepat waktu",
    createdAt: ts,
    updatedAt: ts,
  };
}

test("Unauthenticated user: denied all reads and writes across all collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/customers`).get());
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/payments`).get());
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/wallet_transfers`).get());
  await assertFails(unauthDb.doc(`users/${ALICE_UID}/settings/profile`).get());
  await assertFails(
    unauthDb
      .doc(`users/${ALICE_UID}/customers/cust_1`)
      .set(validCustomerPayload(ALICE_UID))
  );
});

test("Removed expenses collection is denied even for authenticated owner", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection(`users/${ALICE_UID}/expenses`).get());
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/expenses/exp_1`).set({
      userId: ALICE_UID,
      title: "Should fail",
    })
  );
});

test("BusinessSettings: owner create, partial update with receiptFooterText, legacy doc migration & cross-user denial", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = nowPast(10000);

  await assertSucceeds(
    aliceDb
      .doc(`users/${ALICE_UID}/settings/profile`)
      .set(validSettingsPayload(ALICE_UID, created))
  );

  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/settings/profile`).update({
      businessName: "Maju Jaya Net",
      businessPhone: "085700001111",
      bankAccountInfo: "BRI 0099887766 a.n. Maju Jaya",
      receiptFooterText: "Terima kasih! Hubungi CS jika internet bermasalah.",
      remindersEnabled: false,
      updatedAt: nowPast(1000),
    })
  );

  // Reject oversized receiptFooterText (> 240 chars)
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/settings/profile`).update({
      receiptFooterText: "x".repeat(241),
      updatedAt: nowPast(500),
    })
  );

  // Reject ghost field (Shadow Update)
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/settings/profile`).update({
      isAdmin: true,
      updatedAt: nowPast(500),
    })
  );

  // Reject immutable createdAt mutation
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/settings/profile`).update({
      createdAt: nowPast(500),
      updatedAt: nowPast(500),
    })
  );

  // Legacy settings document containing monthlyExpenseBudget can still be updated with receiptFooterText
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().doc(`users/${ALICE_UID}/settings/legacy_profile`).set({
      userId: ALICE_UID,
      businessName: "Old ISP",
      businessPhone: "08111",
      businessAddress: "Jl. Lama",
      bankAccountInfo: "BCA 123",
      monthlyExpenseBudget: 2500000,
      reminderDaysBefore: 3,
      remindersEnabled: true,
      createdAt: created,
      updatedAt: created,
    });
  });

  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/settings/legacy_profile`).update({
      businessName: "Updated ISP",
      receiptFooterText: "Terima kasih telah berlangganan.",
      updatedAt: nowPast(500),
    })
  );

  // Reject cross-user read & write
  await assertFails(bobDb.doc(`users/${ALICE_UID}/settings/profile`).get());
  await assertFails(
    bobDb.doc(`users/${ALICE_UID}/settings/profile`).update({
      businessName: "Hacked ISP",
      updatedAt: nowPast(500),
    })
  );
});

test("Customer: owner CRUD, list query, future timestamp & extra @DocumentId field rejection", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = nowPast(10000);

  await assertSucceeds(
    aliceDb
      .doc(`users/${ALICE_UID}/customers/cust_1`)
      .set(validCustomerPayload(ALICE_UID, created))
  );

  await assertSucceeds(aliceDb.collection(`users/${ALICE_UID}/customers`).get());

  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/customers/cust_1`).update({
      packageName: "50 Mbps Pro",
      monthlyFee: 250000,
      updatedAt: nowPast(1000),
    })
  );

  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/customers/cust_2`).set({
      ...validCustomerPayload(ALICE_UID, created),
      id: "cust_2",
    })
  );

  await assertFails(
    aliceDb
      .doc(`users/${ALICE_UID}/customers/cust_3`)
      .set(validCustomerPayload(ALICE_UID, new Date(Date.now() + 60000)))
  );

  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/customers/cust_4`).set({
      ...validCustomerPayload(ALICE_UID, created),
      dueDay: 31,
    })
  );

  await assertFails(bobDb.collection(`users/${ALICE_UID}/customers`).get());
  await assertFails(bobDb.doc(`users/${ALICE_UID}/customers/cust_1`).delete());
  await assertSucceeds(aliceDb.doc(`users/${ALICE_UID}/customers/cust_1`).delete());
});

test("Payment: relational customer check, edit previous month, wallet validation, cross-user denial", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = nowPast(10000);

  await assertFails(
    aliceDb
      .doc(`users/${ALICE_UID}/payments/cust_missing_2026-11`)
      .set(validPaymentPayload(ALICE_UID, "cust_missing", "2026-11", created))
  );

  await assertSucceeds(
    aliceDb
      .doc(`users/${ALICE_UID}/customers/cust_1`)
      .set(validCustomerPayload(ALICE_UID, created))
  );

  await assertSucceeds(
    aliceDb
      .doc(`users/${ALICE_UID}/payments/cust_1_2026-10`)
      .set(validPaymentPayload(ALICE_UID, "cust_1", "2026-10", created))
  );

  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/payments/cust_1_2026-10`).update({
      amount: 175000,
      paymentMethod: "CASH",
      bankReference: "",
      notes: "Koreksi input bulan lalu",
      updatedAt: nowPast(1000),
    })
  );

  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/payments/cust_1_2026-10`).update({
      paymentMethod: "CRYPTO",
      updatedAt: nowPast(500),
    })
  );

  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/payments/cust_1_2026-10`).update({
      customerId: "cust_other",
      updatedAt: nowPast(500),
    })
  );

  await assertFails(
    aliceDb
      .doc(`users/${ALICE_UID}/payments/cust_1_2026-13`)
      .set(validPaymentPayload(ALICE_UID, "cust_1", "2026-13", created))
  );

  await assertSucceeds(aliceDb.collection(`users/${ALICE_UID}/payments`).get());
  await assertFails(bobDb.collection(`users/${ALICE_UID}/payments`).get());
  await assertFails(
    bobDb.doc(`users/${ALICE_UID}/payments/cust_1_2026-10`).get()
  );
});

test("WalletTransfer: owner create/edit/delete, same-wallet rejection, cross-user denial", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = nowPast(10000);

  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/wallet_transfers/trf_1`).set({
      userId: ALICE_UID,
      yearMonth: "2026-11",
      fromWallet: "CASH",
      toWallet: "BANK_TRANSFER",
      amount: 500000,
      transferDateMillis: Date.now() - 3000,
      notes: "Setor tunai ke rekening BCA",
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/wallet_transfers/trf_bad`).set({
      userId: ALICE_UID,
      yearMonth: "2026-11",
      fromWallet: "CASH",
      toWallet: "CASH",
      amount: 100000,
      transferDateMillis: Date.now() - 3000,
      notes: "Invalid same wallet",
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/wallet_transfers/trf_1`).update({
      amount: 600000,
      notes: "Koreksi nominal setor",
      updatedAt: nowPast(1000),
    })
  );

  await assertSucceeds(aliceDb.collection(`users/${ALICE_UID}/wallet_transfers`).get());
  await assertFails(bobDb.collection(`users/${ALICE_UID}/wallet_transfers`).get());
  await assertFails(bobDb.doc(`users/${ALICE_UID}/wallet_transfers/trf_1`).delete());
  await assertSucceeds(aliceDb.doc(`users/${ALICE_UID}/wallet_transfers/trf_1`).delete());
});
