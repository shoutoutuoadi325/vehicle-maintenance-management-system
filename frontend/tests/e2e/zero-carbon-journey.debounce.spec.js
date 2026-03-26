const { test, expect } = require('@playwright/test');

test('double click answer should only submit one checkin request', async ({ page }) => {
  let checkinCount = 0;

  await page.addInitScript(() => {
    localStorage.setItem('user', JSON.stringify({ id: 101, username: 'test-user' }));
    localStorage.setItem('userRole', 'customer');
    localStorage.setItem('accessToken', 'mock-access-token');
    localStorage.setItem('refreshToken', 'mock-refresh-token');
  });

  await page.route('**/api/gamification/journey/config', async route => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        nodes: [
          { cityIndex: 0, cityName: '成都', requiredMileage: 0, x: 70, y: 470 },
          { cityIndex: 1, cityName: '康定', requiredMileage: 120, x: 285, y: 345 },
          { cityIndex: 2, cityName: '理塘', requiredMileage: 260, x: 470, y: 270 },
          { cityIndex: 3, cityName: '林芝', requiredMileage: 420, x: 680, y: 195 },
          { cityIndex: 4, cityName: '拉萨', requiredMileage: 580, x: 940, y: 95 }
        ]
      })
    });
  });

  await page.route('**/api/gamification/journey/state/me', async route => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        userId: 101,
        totalEnergy: 0,
        currentMileage: 0,
        nodes: [
          { cityIndex: 0, cityName: '成都', requiredMileage: 0, nodeState: 'UNLOCKED' },
          { cityIndex: 1, cityName: '康定', requiredMileage: 120, nodeState: 'LOCKED' },
          { cityIndex: 2, cityName: '理塘', requiredMileage: 260, nodeState: 'LOCKED' },
          { cityIndex: 3, cityName: '林芝', requiredMileage: 420, nodeState: 'LOCKED' },
          { cityIndex: 4, cityName: '拉萨', requiredMileage: 580, nodeState: 'LOCKED' }
        ]
      })
    });
  });

  await page.route('**/api/gamification/journey/quiz?cityIndex=*', async route => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        id: 1,
        cityIndex: 0,
        eventTitle: '遭遇早高峰拥堵',
        eventDescription: '成都城区车流缓慢，如何通过驾驶方式减少不必要的油耗与排放？',
        eventTheme: 'traffic',
        question: '以下哪种行为更环保？',
        options: JSON.stringify({ A: '急加速', B: '平稳驾驶', C: '长怠速', D: '频繁急刹' })
      })
    });
  });

  await page.route('**/api/gamification/journey/checkin', async route => {
    checkinCount += 1;
    await page.waitForTimeout(250);
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        userId: 101,
        quizId: 1,
        correct: true,
        rewardEnergy: 20,
        totalEnergy: 20,
        currentMileage: 20
      })
    });
  });

  await page.goto('/customer/journey');

  await page.locator('circle.city-node.unlocking').first().click();
  const firstOption = page.locator('.option-btn').first();
  await expect(firstOption).toBeVisible();

  await firstOption.dblclick();

  await expect.poll(() => checkinCount).toBe(1);
  await expect(page.locator('.feedback.success')).toContainText('回答正确');
});
