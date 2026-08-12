import { test, expect } from '@playwright/test';

test.describe('Material Management E2E', () => {
  test('creates a material and UI correctly reflects all changes', async ({ page }) => {
    await page.goto('/');

    await page.evaluate(() => localStorage.clear());
    await page.reload();

    await page.getByRole('button', { name: 'Add New Material Form' }).click();

    const testTitle = 'E2E Test Strain ' + Date.now();
    await page.getByLabel(/Material Title/).fill(testTitle);
    await page.getByLabel(/Content \/ Description/).fill('Created by Playwright E2E test');
    await page.getByLabel('Category').selectOption('Viral Strains');
    await page.getByLabel('Status').selectOption('Pending');

    await page.getByRole('button', { name: 'Save Material' }).click();

    await expect(page.locator(`text=${testTitle}`)).toBeVisible();
    await expect(page.locator('text=Created by Playwright E2E test')).toBeVisible();
    await expect(page.locator('text=Viral Strains').first()).toBeVisible();
    await expect(page.locator('text=Pending').first()).toBeVisible();

    // Verify error state correctly reflects lack of title and updates
    await page.getByRole('button', { name: 'Add New Material Form' }).click();
    await page.getByRole('button', { name: 'Save Material' }).click();
    await expect(page.locator('text=Material title is required.')).toBeVisible();

    await page.getByLabel(/Material Title/).fill('Test Update Title');
    await page.getByRole('button', { name: 'Save Material' }).click();
    await expect(page.locator('text=Material description/content is required.')).toBeVisible();
  });
});
