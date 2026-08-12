<script>
  // Props in Svelte 5
  let { selectedMaterial, onBack } = $props();

  let item = $state(null);
  let isLoading = $state(false);
  let errorMsg = $state("");

  // Fetch full details from the API or fallback to passed object
  async function fetchDetails() {
    if (!selectedMaterial || !selectedMaterial.id) {
      item = selectedMaterial;
      return;
    }

    isLoading = true;
    errorMsg = "";
    try {
      // Attempt to retrieve latest data from api
      const response = await fetch(`/api/materials/${selectedMaterial.id}`);
      if (response.ok) {
        item = await response.json();
      } else {
        throw new Error("API returned non-OK status");
      }
    } catch (e) {
      console.warn("Could not fetch material details from API, using client state.", e);
      item = selectedMaterial;
    } finally {
      isLoading = false;
    }
  }

  // Fetch whenselectedMaterial prop changes
  $effect(() => {
    fetchDetails();
  });

  function formatDate(isoString) {
    try {
      const date = new Date(isoString);
      return date.toLocaleDateString("en-US", {
        month: "long",
        day: "numeric",
        year: "numeric",
        hour: "2-digit",
        minute: "2-digit"
      });
    } catch (e) {
      return isoString || "Oct 12, 2023";
    }
  }

  function getBadgeClasses(status) {
    if (status === "Analyzed" || status === "Active") {
      return "bg-[#CCFBF1] text-[#134E4A] border-[#5EEAD4]";
    } else if (status === "Urgent") {
      return "bg-[#FEE2E2] text-[#991B1B] border-[#FCA5A5]";
    } else if (status === "Pending") {
      return "bg-[#FEF3C7] text-[#92400E] border-[#FCD34D]";
    } else {
      return "bg-[#F3F4F6] text-[#374151] border-[#D1D5DB]";
    }
  }
</script>

<div class="bg-surface text-on-surface font-body-md min-h-screen flex flex-col md:flex-row w-full animate-fade-in text-left">
  <!-- Sidebar Navigation Drawer (matches layout of main app for perfect consistency) -->
  <aside class="hidden md:flex flex-col h-screen py-stack-lg bg-surface-container-lowest dark:bg-surface-dim text-secondary dark:text-secondary-fixed font-label-md text-label-md w-64 fixed left-0 top-0 border-r border-outline-variant z-40">
    <div class="px-margin-desktop mb-stack-lg">
      <span class="font-headline-sm text-headline-sm text-primary font-bold">EpiProtocol</span>
    </div>
    <div class="px-margin-desktop mb-stack-sm font-headline-sm text-headline-sm text-on-surface">Categories</div>
    <nav class="flex-1 overflow-y-auto">
      <ul class="space-y-stack-xs">
        <li>
          <button
            onclick={onBack}
            class="w-full flex items-center gap-stack-md p-stack-md mx-stack-sm rounded-full cursor-pointer text-left transition-all text-on-surface-variant hover:bg-surface-container-high"
          >
            <span class="material-symbols-outlined">arrow_back</span>
            Back to Search
          </button>
        </li>
      </ul>
    </nav>
  </aside>

  <!-- Main Content Area -->
  <main class="flex-1 flex flex-col md:ml-64 relative min-h-screen pb-20 md:pb-0">
    <!-- TopAppBar -->
    <header class="w-full top-0 sticky bg-surface dark:bg-inverse-surface border-b border-outline-variant dark:border-outline z-30">
      <div class="flex justify-between items-center px-margin-desktop py-stack-md max-w-container-max mx-auto">
        <button onclick={onBack} class="flex items-center gap-2 text-secondary hover:text-primary transition-colors focus-visible:ring-2 focus-visible:ring-secondary focus-visible:outline-none py-1 px-2 rounded-lg" aria-label="Go back to results page">
          <span class="material-symbols-outlined">arrow_back</span>
          <span class="font-bold">Back to Search</span>
        </button>
        <div class="flex items-center cursor-pointer active:opacity-80" aria-label="Account Menu">
          <span class="material-symbols-outlined text-secondary dark:text-secondary-fixed text-[28px]">account_circle</span>
        </div>
      </div>
    </header>

    <div class="max-w-4xl mx-auto w-full px-margin-mobile md:px-margin-desktop py-stack-lg flex-1">
      {#if isLoading}
        <div class="flex flex-col items-center justify-center py-20 gap-4" id="detail-loader">
          <span class="animate-spin text-secondary material-symbols-outlined text-[48px]">progress_activity</span>
          <p class="font-bold text-on-surface-variant">Loading protocol details...</p>
        </div>
      {:else if item}
        <!-- Material Detail Header Card -->
        <div class="bg-surface-container-lowest rounded-xl border border-outline-variant p-6 md:p-8 shadow-sm flex flex-col gap-6 mb-8">
          <div class="flex flex-col md:flex-row md:items-center justify-between gap-4 border-b border-[#E2E8F0] pb-6">
            <div>
              <div class="flex items-center gap-2 mb-2">
                <span class="inline-flex items-center px-2.5 py-1 rounded-full font-label-md text-[11px] font-bold border {getBadgeClasses(item.status)}">
                  {item.status === 'Analyzed' ? 'Active' : item.status}
                </span>
                <span class="text-xs text-on-surface-variant font-medium">EP-{item.id}</span>
              </div>
              <h1 class="font-headline-lg text-headline-lg font-bold text-on-surface" id="detail-title">{item.title}</h1>
            </div>
          </div>

          <!-- Metadata Grid -->
          <div class="grid grid-cols-1 sm:grid-cols-3 gap-6 bg-surface-container-low p-4 rounded-xl border border-[#EFF6FF]">
            <div class="flex flex-col gap-1">
              <span class="text-xs text-on-surface-variant font-bold uppercase tracking-wider font-label-sm">Category</span>
              <span class="text-body-md font-semibold text-on-surface flex items-center gap-1">
                <span class="material-symbols-outlined text-[18px] text-secondary">database</span>
                {item.category || "Viral Strains"}
              </span>
            </div>
            <div class="flex flex-col gap-1">
              <span class="text-xs text-on-surface-variant font-bold uppercase tracking-wider font-label-sm">Author</span>
              <span class="text-body-md font-semibold text-on-surface flex items-center gap-1">
                <span class="material-symbols-outlined text-[18px] text-secondary">person</span>
                Dr. Sarah Chen
              </span>
            </div>
            <div class="flex flex-col gap-1">
              <span class="text-xs text-on-surface-variant font-bold uppercase tracking-wider font-label-sm">Last Updated</span>
              <span class="text-body-md font-semibold text-on-surface flex items-center gap-1">
                <span class="material-symbols-outlined text-[18px] text-secondary">calendar_today</span>
                {formatDate(item.updated_at)}
              </span>
            </div>
          </div>

          <!-- Content Section -->
          <div class="flex flex-col gap-4">
            <h2 class="font-headline-sm text-headline-sm font-bold text-on-surface border-b border-[#E2E8F0] pb-2">
              Protocol Overview & SOP
            </h2>
            <div class="font-body-lg text-body-lg text-on-surface-variant leading-relaxed whitespace-pre-wrap py-2" id="detail-content">
              {item.content || "No protocol details provided."}
            </div>
          </div>

          <!-- Document Control Info -->
          <div class="mt-4 pt-6 border-t border-[#E2E8F0] flex flex-wrap gap-4 justify-between items-center text-xs text-on-surface-variant">
            <span>Document Control ID: EP-{item.id}-DOC</span>
            <span>Security Classification: Restricted</span>
          </div>
        </div>
      {:else}
        <div class="bg-surface-container-lowest rounded-lg border border-outline-variant p-stack-xl text-center">
          <span class="material-symbols-outlined text-[48px] text-error mb-2">error</span>
          <p class="font-headline-sm font-semibold text-on-surface mb-1">Protocol not found</p>
          <p class="text-body-md text-on-surface-variant">We were unable to locate this epidemiological protocol.</p>
          <button onclick={onBack} class="mt-4 bg-secondary text-on-primary py-2 px-4 rounded-xl font-bold hover:bg-[#004395] transition-all">
            Return to Search
          </button>
        </div>
      {/if}
    </div>
  </main>
</div>
